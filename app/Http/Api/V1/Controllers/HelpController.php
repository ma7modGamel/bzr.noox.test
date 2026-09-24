<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Controllers;

use App\Mail\SupportMessageMail;
use App\Modules\Content\Enums\LegalPageSlug;
use App\Modules\Content\Models\LegalPage;
use App\Modules\Identity\Models\User;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Mail;

/** C29 — الأسئلة من الخادم ورسالة الدعم بالبريد. */
final class HelpController
{
    public function faqs(): JsonResponse
    {
        // DEC-051 — الأسئلة الشائعة تُدار من اللوحة: كل عنوان h3 سؤال وما بعده الإجابة.
        $page = LegalPage::query()->where('slug', LegalPageSlug::Faq->value)->first()?->currentVersion();

        return new JsonResponse(['data' => $page === null ? config('mobile.support_faqs', []) : self::faqItems($page->safeBody())]);
    }

    /** @return list<array{code: string, title: string, body: string}> */
    private static function faqItems(string $html): array
    {
        $parts = preg_split('/<h3[^>]*>/i', $html) ?: [];
        array_shift($parts);
        $items = [];
        foreach ($parts as $index => $part) {
            [$question, $answer] = array_pad(preg_split('/<\/h3>/i', $part, 2) ?: [], 2, '');
            $items[] = [
                'code' => 'FAQ_'.($index + 1),
                'title' => trim(html_entity_decode(strip_tags($question))),
                'body' => trim(preg_replace('/\s+/u', ' ', html_entity_decode(strip_tags(str_replace(['</p>', '<br>', '</li>'], ["\n", "\n", "\n"], $answer)))) ?? ''),
            ];
        }

        return $items;
    }

    public function message(Request $request): JsonResponse
    {
        $data = $request->validate([
            'subject' => ['required', 'string', 'min:5', 'max:120'],
            'message' => ['required', 'string', 'min:10', 'max:2000'],
        ]);

        /** @var User $user */
        $user = $request->user();
        Mail::to(config('mail.support_address'))->queue(new SupportMessageMail(
            userId: $user->getKey(),
            customerName: $user->name,
            customerEmail: $user->email,
            supportSubject: $data['subject'],
            supportMessage: $data['message'],
        ));

        return new JsonResponse(status: 202);
    }
}
