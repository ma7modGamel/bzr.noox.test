<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Controllers;

use App\Modules\Communication\Models\Conversation;
use App\Modules\Communication\Models\Message;
use App\Modules\Communication\Services\ConversationService;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderMedia;
use App\Modules\Providers\Models\ProviderProfile;
use App\Support\Localization;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Storage;
use Symfony\Component\HttpFoundation\StreamedResponse;

final class ConversationController
{
    public function index(Request $request): JsonResponse
    {
        $user = $request->user();
        $profileId = $user->providerProfile?->getKey();

        $conversations = Conversation::query()
            ->withCount(['messages as unread_count' => fn ($query) => $query->where('sender_user_id', '!=', $user->getKey())->whereNull('read_at')])
            ->with([
                'order.category', 'order.problemType', 'order.area', 'providerProfile.user', 'customer',
                'messages' => fn ($query) => $query->reorder()->latest('id')->limit(1),
            ])
            ->where(fn ($query) => $query
                ->where('customer_id', $user->getKey())
                ->when($profileId !== null, fn ($query) => $query->orWhere('provider_profile_id', $profileId)))
            ->orderByDesc('updated_at')
            ->orderByDesc('id')
            ->paginate(20);

        return new JsonResponse([
            'data' => $conversations->getCollection()->map(
                fn (Conversation $conversation) => $this->conversation($conversation, $user->getKey()),
            ),
            'meta' => [
                'current_page' => $conversations->currentPage(),
                'last_page' => $conversations->lastPage(),
            ],
        ]);
    }

    public function store(Request $request, ConversationService $service): JsonResponse
    {
        $data = $request->validate([
            'order_id' => ['required', 'integer'],
            'provider_profile_id' => ['required', 'integer'],
        ]);

        $conversation = $service->start(
            $request->user(),
            Order::query()->findOrFail($data['order_id']),
            ProviderProfile::query()->findOrFail($data['provider_profile_id']),
        );
        $conversation->loadCount(['messages as unread_count' => fn ($query) => $query->where('sender_user_id', '!=', $request->user()->getKey())->whereNull('read_at')]);

        return new JsonResponse([
            'data' => $this->conversation(
                $conversation->load(['order.category', 'order.problemType', 'order.area', 'providerProfile.user', 'customer']),
                $request->user()->getKey(),
            ),
        ], 201);
    }

    public function messages(Request $request, Conversation $conversation, ConversationService $service): JsonResponse
    {
        $service->assertParticipant($conversation, $request->user());

        $conversation->load(['order.category', 'order.problemType', 'order.area', 'providerProfile.user', 'customer']);
        $messages = $conversation->messages()->with('media')->reorder()->latest('id')->paginate(50);
        $unread = $messages->getCollection()->filter(fn (Message $message): bool => $message->sender_user_id !== $request->user()->getKey() && $message->read_at === null);
        if ($unread->isNotEmpty()) {
            $readAt = now();
            $conversation->messages()->whereIn('id', $unread->modelKeys())->whereNull('read_at')->update(['read_at' => $readAt]);
            $unread->each(fn (Message $message) => $message->setAttribute('read_at', $readAt));
        }
        $conversation->loadCount(['messages as unread_count' => fn ($query) => $query->where('sender_user_id', '!=', $request->user()->getKey())->whereNull('read_at')]);

        return new JsonResponse([
            'conversation' => $this->conversation($conversation, $request->user()->getKey()),
            'data' => $messages->getCollection()
                ->reverse()
                ->values()
                ->map(fn (Message $message) => $this->message($message, $request->user()->getKey())),
            'meta' => ['current_page' => $messages->currentPage(), 'last_page' => $messages->lastPage()],
        ]);
    }

    public function send(Request $request, Conversation $conversation, ConversationService $service): JsonResponse
    {
        $data = $request->validate([
            'body' => ['nullable', 'string', 'max:1000', 'required_without:media_ids'],
            'media_ids' => ['nullable', 'array', 'max:5', 'required_without:body'],
            'media_ids.*' => ['integer', 'distinct'],
        ]);

        $message = $service->send(
            $conversation,
            $request->user(),
            trim((string) ($data['body'] ?? '')),
            array_map('intval', $data['media_ids'] ?? []),
        );

        return new JsonResponse(['data' => $this->message($message, $request->user()->getKey())], 201);
    }

    public function image(Request $request, Conversation $conversation, Message $message, OrderMedia $media, ConversationService $service): StreamedResponse
    {
        $service->assertParticipant($conversation, $request->user());
        abort_unless($message->conversation_id === $conversation->id && $message->media()->whereKey($media->id)->where('type', 'IMAGE')->exists(), 404);
        abort_unless(Storage::disk('local')->exists($media->path), 404);

        return Storage::disk('local')->response($media->path, headers: ['Cache-Control' => 'no-store, private', 'X-Content-Type-Options' => 'nosniff']);
    }

    /** @return array<string, mixed> */
    private function conversation(Conversation $conversation, int $viewerId): array
    {
        return [
            'id' => $conversation->getKey(),
            'order' => [
                'id' => $conversation->order_id,
                'number' => $conversation->order?->number,
                'status' => $conversation->order?->status?->value,
                'status_label' => $conversation->order?->statusLabel(),
                'category' => Localization::field($conversation->order?->category),
                'problem_type' => Localization::field($conversation->order?->problemType),
                'area' => Localization::field($conversation->order?->area),
            ],
            'provider' => [
                'id' => $conversation->provider_profile_id,
                'name' => $conversation->providerProfile?->user?->name,
                'avatar_path' => $conversation->providerProfile?->user?->avatar_path,
                'is_verified' => $conversation->providerProfile?->isVerified() ?? false,
                'available_now' => (bool) $conversation->providerProfile?->available_now,
            ],
            'customer' => [
                'id' => $conversation->customer_id,
                'name' => $conversation->customer?->name,
                'avatar_path' => $conversation->customer?->avatar_path,
                ...($viewerId !== $conversation->customer_id ? ['rating_avg' => $conversation->customer?->customer_rating_avg] : []),
            ],
            'status' => $conversation->status->value,
            'unread_count' => (int) ($conversation->unread_count ?? 0),
            'last_message' => $conversation->relationLoaded('messages') && $conversation->messages->isNotEmpty()
                ? $this->message($conversation->messages->first(), $viewerId)
                : null,
        ];
    }

    /** @return array<string, mixed> */
    private function message(Message $message, int $viewerId): array
    {
        return [
            'id' => $message->getKey(),
            'sender_user_id' => $message->sender_user_id,
            'is_mine' => $message->sender_user_id === $viewerId,
            'body' => $message->body,
            'was_masked' => $message->was_masked,
            'media' => $message->relationLoaded('media') ? $message->media->map(fn ($media) => [
                'id' => $media->getKey(),
                'type' => $media->type,
                'content_url' => route('api.conversations.image', ['conversation' => $message->conversation_id, 'message' => $message->id, 'media' => $media->id]),
            ])->values() : [],
            'created_at' => $message->created_at?->toIso8601String(),
            'read_at' => $message->read_at?->toIso8601String(),
        ];
    }
}
