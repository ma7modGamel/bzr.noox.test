<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Controllers;

use App\Modules\Communication\Models\Conversation;
use App\Modules\Communication\Models\Message;
use App\Modules\Communication\Services\ConversationService;
use App\Modules\Orders\Models\Order;
use App\Modules\Providers\Models\ProviderProfile;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

final class ConversationController
{
    public function index(Request $request): JsonResponse
    {
        $user = $request->user();
        $profileId = $user->providerProfile?->getKey();

        $conversations = Conversation::query()
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
                'category' => $conversation->order?->category?->name,
                'problem_type' => $conversation->order?->problemType?->name,
                'area' => $conversation->order?->area?->name,
            ],
            'provider' => [
                'id' => $conversation->provider_profile_id,
                'name' => $conversation->providerProfile?->user?->name,
                'avatar_path' => $conversation->providerProfile?->user?->avatar_path,
                'is_verified' => $conversation->providerProfile?->isVerified() ?? false,
            ],
            'customer' => [
                'id' => $conversation->customer_id,
                'name' => $conversation->customer?->name,
                'avatar_path' => $conversation->customer?->avatar_path,
                'rating_avg' => $conversation->customer?->customer_rating_avg,
            ],
            'status' => $conversation->status->value,
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
            ])->values() : [],
            'created_at' => $message->created_at?->toIso8601String(),
        ];
    }
}
