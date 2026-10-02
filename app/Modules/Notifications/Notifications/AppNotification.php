<?php

declare(strict_types=1);

namespace App\Modules\Notifications\Notifications;

use App\Modules\Notifications\Channels\AppDatabaseChannel;
use App\Modules\Notifications\Data\NotificationContent;
use Illuminate\Bus\Queueable;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Notifications\Messages\MailMessage;
use Illuminate\Notifications\Notification;

/**
 * إشعار واحد من كتالوج 17. الداخلي يُحفظ فورًا (`notifyNow`)، والبريد يمر بالطابور.
 */
final class AppNotification extends Notification implements ShouldQueue
{
    use Queueable;

    /** @param list<string> $channels */
    public function __construct(
        public readonly NotificationContent $content,
        private readonly array $channels = [AppDatabaseChannel::class],
    ) {
        $this->afterCommit();
    }

    /** @return list<string> */
    public function via(object $notifiable): array
    {
        return $this->channels;
    }

    public function databaseType(object $notifiable): string
    {
        return $this->content->code->value;
    }

    /** @return array<string, mixed> */
    public function toArray(object $notifiable): array
    {
        return $this->content->toArray();
    }

    public function toMail(object $notifiable): MailMessage
    {
        $mail = (new MailMessage)
            ->subject($this->content->title.' — '.config('app.name'))
            ->greeting($this->content->title)
            ->line($this->content->body);

        if (($link = $this->content->webLink()) !== null) {
            $mail->action('فتح في تطبيق '.config('app.name'), $link);
        }

        return $mail->salutation('فريق '.config('app.name'));
    }
}
