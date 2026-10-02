<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Controllers;

use App\Http\Api\V1\Resources\ProviderApplicationResource;
use App\Http\Requests\Api\V1\SubmitProviderApplicationRequest;
use App\Modules\Identity\Models\User;
use App\Modules\Providers\Actions\SubmitProviderApplicationAction;
use App\Support\Exceptions\EmailNotVerifiedException;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

final class ProviderApplicationController
{
    public function show(Request $request): ProviderApplicationResource
    {
        /** @var User $user */
        $user = $request->user();
        if (! $user->hasVerifiedEmail()) {
            throw EmailNotVerifiedException::make();
        }

        $profile = $user->providerProfile()
            ->with(['user', 'categories:id,name', 'specialties:id,name', 'areas:id,name', 'documents'])
            ->first();

        return new ProviderApplicationResource($profile);
    }

    public function store(
        SubmitProviderApplicationRequest $request,
        SubmitProviderApplicationAction $action,
    ): JsonResponse {
        /** @var User $user */
        $user = $request->user();
        $created = ! $user->providerProfile()->exists();
        $profile = $action->execute($user, $request->applicationData());

        return (new ProviderApplicationResource($profile))
            ->response()
            ->setStatusCode($created ? 201 : 200);
    }
}
