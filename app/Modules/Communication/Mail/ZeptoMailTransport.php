<?php

declare(strict_types=1);

namespace App\Modules\Communication\Mail;

use Illuminate\Support\Facades\Http;
use RuntimeException;
use Symfony\Component\Mailer\SentMessage;
use Symfony\Component\Mailer\Transport\AbstractTransport;
use Symfony\Component\Mime\Address;
use Symfony\Component\Mime\Email;
use Symfony\Component\Mime\MessageConverter;

/**
 * DEC-049 — إرسال البريد التلقائي عبر ZeptoMail API (Zoho).
 * المفتاح من env (`ZEPTOMAIL_API_KEY`) ولا يظهر في أي استثناء أو سجل.
 */
final class ZeptoMailTransport extends AbstractTransport
{
    public function __construct(
        private readonly string $url,
        private readonly ?string $key,
        private readonly int $timeout = 10,
    ) {
        parent::__construct();
    }

    protected function doSend(SentMessage $message): void
    {
        if ($this->key === null || $this->key === '') {
            throw new RuntimeException('ZEPTOMAIL_API_KEY غير مضبوط.');
        }

        $email = MessageConverter::toEmail($message->getOriginalMessage());
        $from = $email->getFrom()[0] ?? throw new RuntimeException('Missing sender.');

        $payload = array_filter([
            'from' => $this->address($from),
            'to' => array_map(fn (Address $address): array => ['email_address' => $this->address($address)], $email->getTo()),
            'cc' => array_map(fn (Address $address): array => ['email_address' => $this->address($address)], $email->getCc()),
            'bcc' => array_map(fn (Address $address): array => ['email_address' => $this->address($address)], $email->getBcc()),
            'reply_to' => array_map(fn (Address $address): array => $this->address($address), $email->getReplyTo()),
            'subject' => (string) $email->getSubject(),
            'htmlbody' => $this->html($email),
            'textbody' => $email->getTextBody() === null ? null : (string) $email->getTextBody(),
        ], static fn (mixed $value): bool => $value !== null && $value !== []);

        $response = Http::timeout($this->timeout)
            ->acceptJson()
            ->withHeaders(['Authorization' => 'Zoho-enczapikey '.$this->key])
            ->post($this->url, $payload);

        if (! $response->successful()) {
            throw new RuntimeException('ZeptoMail rejected the message (HTTP '.$response->status().').');
        }
    }

    public function __toString(): string
    {
        return 'zeptomail';
    }

    /** @return array{address: string, name?: string} */
    private function address(Address $address): array
    {
        return array_filter(['address' => $address->getAddress(), 'name' => $address->getName()], static fn (string $value): bool => $value !== '');
    }

    private function html(Email $email): ?string
    {
        $body = $email->getHtmlBody();

        return $body === null ? null : (string) $body;
    }
}
