<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Controllers;

use App\Http\Middleware\ResolveAppMode;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Relations\MorphMany;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Notifications\DatabaseNotification;

/** C32 — القائمة الداخلية تبقى متاحة حتى عند فشل Push (EC-22)، ومصفّاة حسب `X-App-Mode` (DEC-058). */
final class NotificationController
{
    public function index(Request $request): JsonResponse
    {
        $notifications = $this->forMode($request->user()->notifications(), $request)->paginate(20);

        return new JsonResponse([
            'data' => $notifications->getCollection()->map(
                static fn (DatabaseNotification $notification): array => [
                    'id' => $notification->getKey(),
                    'code' => $notification->data['code'] ?? class_basename($notification->type),
                    'title' => $notification->data['title'] ?? '',
                    'body' => $notification->data['body'] ?? '',
                    'deep_link' => $notification->data['deep_link'] ?? null,
                    'read_at' => $notification->read_at?->toIso8601String(),
                    'created_at' => $notification->created_at?->toIso8601String(),
                ],
            )->values(),
            'meta' => [
                'current_page' => $notifications->currentPage(),
                'last_page' => $notifications->lastPage(),
                'unread_count' => self::unreadCount($request),
            ],
        ]);
    }

    public function read(Request $request): JsonResponse
    {
        $data = $request->validate([
            'notification_ids' => ['required', 'array', 'min:1', 'max:100'],
            'notification_ids.*' => ['required', 'uuid', 'distinct'],
        ]);

        $request->user()->unreadNotifications()
            ->whereIn('id', $data['notification_ids'])
            ->update(['read_at' => now()]);

        return new JsonResponse(status: 204);
    }

    /** يُستخدم أيضًا في رئيسية الفني (P08). */
    public static function unreadCount(Request $request): int
    {
        return (new self)->forMode($request->user()->unreadNotifications(), $request)->count();
    }

    /** الإشعارات القديمة بلا `app_mode` تُعد إشعارات عميل. */
    private function forMode(MorphMany $query, Request $request): MorphMany
    {
        $mode = (string) $request->attributes->get('app_mode', ResolveAppMode::CUSTOMER);

        return $query->where(fn (Builder $q) => $mode === ResolveAppMode::CUSTOMER
            ? $q->where('app_mode', $mode)->orWhereNull('app_mode')
            : $q->where('app_mode', $mode));
    }
}
