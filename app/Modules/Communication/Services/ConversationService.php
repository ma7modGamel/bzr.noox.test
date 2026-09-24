<?php

declare(strict_types=1);

namespace App\Modules\Communication\Services;

use App\Modules\Communication\Enums\ConversationStatus;
use App\Modules\Communication\Models\Conversation;
use App\Modules\Communication\Models\Message;
use App\Modules\Identity\Models\User;
use App\Modules\Offers\Enums\OfferStatus;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderMedia;
use App\Modules\Providers\Models\ProviderProfile;
use App\Support\Exceptions\BusinessRuleViolationException;
use Illuminate\Support\Facades\DB;

final readonly class ConversationService
{
    public function __construct(private ContactMasker $masker) {}

    public function start(User $customer, Order $order, ProviderProfile $provider): Conversation
    {
        if ($order->customer_id !== $customer->getKey() || $order->status !== OrderStatus::Open) {
            throw BusinessRuleViolationException::rule('BR-100', 'لا يمكن بدء هذه المحادثة.');
        }

        $hasOffer = $order->offers()
            ->where('provider_profile_id', $provider->getKey())
            ->where('status', OfferStatus::Submitted->value)
            ->exists();

        if (! $hasOffer) {
            throw BusinessRuleViolationException::rule('BR-100', 'المحادثة متاحة مع مقدم خدمة أرسل عرضًا فقط.');
        }

        return Conversation::query()->firstOrCreate([
            'order_id' => $order->getKey(),
            'provider_profile_id' => $provider->getKey(),
        ], [
            'customer_id' => $customer->getKey(),
            'status' => ConversationStatus::Open,
        ]);
    }

    public function ensureAssignedConversation(Order $order, int $providerProfileId): Conversation
    {
        return Conversation::query()->updateOrCreate([
            'order_id' => $order->getKey(),
            'provider_profile_id' => $providerProfileId,
        ], [
            'customer_id' => $order->customer_id,
            'status' => ConversationStatus::Open,
        ]);
    }

    /** @param list<int> $mediaIds */
    public function send(Conversation $conversation, User $sender, string $body, array $mediaIds): Message
    {
        $this->assertParticipant($conversation, $sender);

        if ($conversation->status !== ConversationStatus::Open) {
            throw BusinessRuleViolationException::rule('BR-102', 'المحادثة للقراءة فقط.');
        }

        $order = $conversation->order;
        $masked = $order->status === OrderStatus::Open
            ? $this->masker->mask($body)
            : ['body' => $body, 'masked' => false];

        return DB::transaction(function () use ($conversation, $sender, $body, $mediaIds, $masked, $order): Message {
            $media = collect();

            if ($mediaIds !== []) {
                if ($order->status === OrderStatus::Open) {
                    throw BusinessRuleViolationException::rule('BR-101', 'الصور متاحة بعد تأكيد الطلب فقط.');
                }

                $media = OrderMedia::query()
                    ->whereIn('id', $mediaIds)
                    ->where('uploaded_by', $sender->getKey())
                    ->whereNull('order_id')
                    ->where('type', 'IMAGE')
                    ->where(fn ($query) => $query->whereNull('expires_at')->orWhere('expires_at', '>', now()))
                    ->lockForUpdate()
                    ->get();

                if ($media->count() !== count(array_unique($mediaIds))) {
                    throw BusinessRuleViolationException::rule('BR-101', 'إحدى صور الرسالة غير متاحة.');
                }
            }

            $message = Message::query()->create([
                'conversation_id' => $conversation->getKey(),
                'sender_user_id' => $sender->getKey(),
                'body' => $masked['body'],
                'original_body_encrypted' => $masked['masked'] ? $body : null,
                'was_masked' => $masked['masked'],
            ]);

            if ($media->isNotEmpty()) {
                OrderMedia::query()->whereKey($media->modelKeys())->update([
                    'order_id' => $order->getKey(),
                    'expires_at' => null,
                ]);
                $message->media()->attach($media->modelKeys());
            }

            $conversation->touch();

            return $message->load('media');
        });
    }

    public function assertParticipant(Conversation $conversation, User $user): void
    {
        $providerUserId = $conversation->providerProfile()->value('user_id');

        abort_unless(
            $conversation->customer_id === $user->getKey() || $providerUserId === $user->getKey(),
            404,
        );
    }
}
