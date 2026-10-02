<?php

declare(strict_types=1);

namespace App\Http\Requests\Api\V1;

use App\Modules\Catalog\Models\ProblemType;
use App\Modules\Geography\Models\Area;
use App\Modules\Providers\Data\ProviderApplicationData;
use App\Modules\Providers\Enums\PayoutMethod;
use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Validation\Rule;
use Illuminate\Validation\Validator;

final class SubmitProviderApplicationRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, list<mixed>> */
    public function rules(): array
    {
        return [
            'experience_years' => ['required', 'integer', 'between:0,50'],
            'bio' => ['nullable', 'string', 'max:300'],
            'category_ids' => ['required', 'array', 'min:1'],
            'category_ids.*' => [
                'integer', 'distinct',
                Rule::exists('categories', 'id')->where('is_active', true),
            ],
            'specialty_ids' => ['required', 'array', 'min:1'],
            'specialty_ids.*' => ['integer', 'distinct'],
            'area_ids' => ['required', 'array', 'min:1'],
            'area_ids.*' => ['integer', 'distinct'],
            'payout_method' => ['required', Rule::enum(PayoutMethod::class)],
            'payout_details' => ['required', 'string', 'max:255'],
            'profile_photo_media_id' => ['nullable', 'integer'],
            'id_front_media_id' => ['nullable', 'integer'],
            'id_back_media_id' => ['nullable', 'integer'],
        ];
    }

    public function withValidator(Validator $validator): void
    {
        $validator->after(function (Validator $validator): void {
            if ($validator->errors()->isNotEmpty()) {
                return;
            }

            $categoryIds = array_map('intval', $this->array('category_ids'));
            $specialtyIds = array_map('intval', $this->array('specialty_ids'));
            $validSpecialties = ProblemType::query()
                ->where('is_active', true)
                ->whereIn('category_id', $categoryIds)
                ->whereIn('id', $specialtyIds)
                ->count();
            if ($validSpecialties !== count($specialtyIds)) {
                $validator->errors()->add('specialty_ids', 'اختر تخصصات مفعلة تابعة للفئات المحددة.');
            }

            $areaIds = array_map('intval', $this->array('area_ids'));
            $validAreas = Area::query()->where('is_active', true)->whereIn('id', $areaIds)->count();
            if ($validAreas !== count($areaIds)) {
                $validator->errors()->add('area_ids', 'اختر مناطق عمل مفعلة.');
            }
        });
    }

    public function applicationData(): ProviderApplicationData
    {
        $data = $this->validated();

        return new ProviderApplicationData(
            experienceYears: (int) $data['experience_years'],
            bio: isset($data['bio']) && trim((string) $data['bio']) !== '' ? trim((string) $data['bio']) : null,
            categoryIds: array_map('intval', $data['category_ids']),
            specialtyIds: array_map('intval', $data['specialty_ids']),
            areaIds: array_map('intval', $data['area_ids']),
            payoutMethod: PayoutMethod::from($data['payout_method']),
            payoutDetails: trim((string) $data['payout_details']),
            profilePhotoMediaId: isset($data['profile_photo_media_id']) ? (int) $data['profile_photo_media_id'] : null,
            idFrontMediaId: isset($data['id_front_media_id']) ? (int) $data['id_front_media_id'] : null,
            idBackMediaId: isset($data['id_back_media_id']) ? (int) $data['id_back_media_id'] : null,
        );
    }

    /** @return array<string, string> */
    public function messages(): array
    {
        return [
            'experience_years.between' => 'سنوات الخبرة يجب أن تكون بين 0 و50.',
            'bio.max' => 'النبذة يجب ألا تتجاوز 300 حرف.',
            'category_ids.required' => 'اختر فئة واحدة على الأقل.',
            'category_ids.min' => 'اختر فئة واحدة على الأقل.',
            'specialty_ids.required' => 'اختر تخصصًا واحدًا على الأقل.',
            'specialty_ids.min' => 'اختر تخصصًا واحدًا على الأقل.',
            'area_ids.required' => 'اختر منطقة عمل واحدة على الأقل.',
            'area_ids.min' => 'اختر منطقة عمل واحدة على الأقل.',
            'payout_method.required' => 'اختر طريقة استلام المستحقات.',
            'payout_details.required' => 'أدخل بيانات استلام المستحقات.',
        ];
    }
}
