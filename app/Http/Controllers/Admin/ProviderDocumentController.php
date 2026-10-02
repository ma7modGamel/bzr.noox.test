<?php

declare(strict_types=1);

namespace App\Http\Controllers\Admin;

use App\Modules\Identity\Models\Admin;
use App\Modules\Providers\Models\ProviderDocument;
use Illuminate\Support\Facades\Storage;
use Symfony\Component\HttpFoundation\StreamedResponse;

/**
 * مستندات هوية الفني للإدارة فقط (23، 32، DEC-060): بلا رابط عام، ولا تخزين مؤقت، ولا فهرسة.
 */
final class ProviderDocumentController
{
    public function __invoke(ProviderDocument $document): StreamedResponse
    {
        $admin = auth('admin')->user();
        abort_unless($admin instanceof Admin && $admin->is_active, 403);
        abort_unless(Storage::disk('local')->exists($document->path), 404);

        return Storage::disk('local')->response($document->path, null, [
            'Cache-Control' => 'no-store, private',
            'X-Robots-Tag' => 'noindex, nofollow',
        ]);
    }
}
