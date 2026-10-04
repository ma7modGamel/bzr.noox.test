<?php

declare(strict_types=1);

namespace Database\Seeders;

use App\Modules\Catalog\Models\Category;
use App\Modules\Catalog\Models\ProblemType;
use App\Modules\Customers\Models\CustomerAddress;
use App\Modules\Geography\Models\Area;
use App\Modules\Geography\Models\City;
use App\Modules\Identity\Enums\UserStatus;
use App\Modules\Identity\Models\User;
use App\Modules\Orders\Models\Order;
use App\Modules\Providers\Enums\EmploymentType;
use App\Modules\Providers\Enums\PayoutMethod;
use App\Modules\Providers\Enums\ProviderStatus;
use App\Modules\Providers\Models\ProviderProfile;
use Illuminate\Database\Seeder;
use Illuminate\Support\Collection;
use Illuminate\Support\Facades\Storage;

/**
 * بيانات تجريبية للعرض: أسعار استرشادية، موظفون، فنيون مستقلون، متقدمون، عملاء وطلبات.
 * الأسعار هنا ليست OD-13؛ تُستبدل بالاستيراد من اللوحة قبل الإطلاق (DEP-OPS-03).
 * كلمة مرور كل الحسابات التجريبية: Bremo-2026.
 */
final class DemoSeeder extends Seeder
{
    public const PASSWORD = 'Bremo-2026';

    private const PRICE_NOTE = 'سعر تجريبي — يُستبدل بنطاقات OD-13';

    /** @var array<string, array{0: int, 1: int}> الفئة ← [أقل حد أدنى، أعلى حد أقصى] بالجنيه */
    private const PRICE_BANDS = [
        'سباكة' => [150, 900],
        'كهرباء' => [150, 1200],
        'تكييفات' => [250, 2500],
        'أجهزة منزلية' => [200, 1500],
        'نجارة' => [150, 1200],
        'دهانات' => [800, 15000],
        'تركيبات منزلية متفرقة' => [100, 600],
        'ألوميتال وزجاج' => [150, 1500],
        'حدادة' => [200, 2000],
        'سيراميك وأرضيات' => [300, 3000],
        'دش وستالايت' => [100, 700],
        'تنظيف خزانات المياه' => [300, 800],
        'مكافحة حشرات' => [300, 1200],
    ];

    /** @var list<array{name: string, categories: list<string>, years: int, bio: string}> */
    private const EMPLOYEES = [
        ['name' => 'محمود السيد', 'categories' => ['سباكة'], 'years' => 12, 'bio' => 'سباك خبرة في التسريبات والأدوات الصحية.'],
        ['name' => 'أحمد عبد الله', 'categories' => ['سباكة', 'تركيبات منزلية متفرقة'], 'years' => 7, 'bio' => 'سباكة وتركيبات منزلية.'],
        ['name' => 'إبراهيم فتحي', 'categories' => ['كهرباء'], 'years' => 15, 'bio' => 'كهربائي تشطيبات ولوحات.'],
        ['name' => 'مصطفى عادل', 'categories' => ['كهرباء', 'تركيبات منزلية متفرقة'], 'years' => 5, 'bio' => 'إضاءة ونجف وتعليق شاشات.'],
        ['name' => 'خالد منصور', 'categories' => ['تكييفات'], 'years' => 10, 'bio' => 'صيانة وتركيب تكييفات بكل الماركات.'],
        ['name' => 'ياسر حمدي', 'categories' => ['تكييفات', 'أجهزة منزلية'], 'years' => 8, 'bio' => 'تكييفات وثلاجات.'],
        ['name' => 'عمرو شعبان', 'categories' => ['أجهزة منزلية'], 'years' => 9, 'bio' => 'غسالات وبوتاجازات وميكروويف.'],
        ['name' => 'سامح رضا', 'categories' => ['نجارة'], 'years' => 18, 'bio' => 'نجار أبواب ومطابخ.'],
        ['name' => 'هاني جمال', 'categories' => ['دهانات'], 'years' => 11, 'bio' => 'دهانات حديثة ومعالجة رطوبة.'],
        ['name' => 'وليد عصام', 'categories' => ['ألوميتال وزجاج', 'نجارة'], 'years' => 6, 'bio' => 'ألوميتال وزجاج وسلك.'],
    ];

    /** @var list<array{name: string, categories: list<string>, years: int, bio: string}> */
    private const INDEPENDENTS = [
        ['name' => 'شريف نبيل', 'categories' => ['سباكة', 'كهرباء'], 'years' => 14, 'bio' => 'فني مستقل سباكة وكهرباء.'],
        ['name' => 'طارق يوسف', 'categories' => ['تكييفات'], 'years' => 9, 'bio' => 'فني تكييف مستقل.'],
        ['name' => 'كريم ممدوح', 'categories' => ['دهانات', 'تركيبات منزلية متفرقة'], 'years' => 4, 'bio' => 'دهانات وتركيبات.'],
    ];

    /** @var list<string> */
    private const CUSTOMERS = [
        'منى حسن', 'سارة محمد', 'محمد علي', 'نهى إبراهيم', 'هشام فاروق', 'دينا سمير', 'علي مجدي',
    ];

    /** صورة PNG بحجم 1×1 بديلًا لصور البطاقة في الطلبات قيد المراجعة. */
    private const PLACEHOLDER_PNG = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=';

    private City $city;

    /** @var Collection<int, Area> */
    private Collection $areas;

    /** @var Collection<string, Category> */
    private Collection $categories;

    public function run(): void
    {
        $this->city = City::query()->where('name', 'دمياط الجديدة')->firstOrFail();
        $this->areas = Area::query()->where('city_id', $this->city->id)->where('is_active', true)->orderBy('sort')->get();
        $this->categories = Category::query()->get()->keyBy('name');

        $priced = $this->seedPrices();
        $this->seedEmployees();
        $this->seedIndependents();
        $this->seedApplicants();
        $customers = $this->seedCustomers();
        $this->seedOpenOrders($customers);

        $this->command?->info("بيانات تجريبية: {$priced} سعرًا، ".count(self::EMPLOYEES).' موظفين، '
            .count(self::INDEPENDENTS).' مستقلين، '.$customers->count().' عملاء. كلمة المرور: '.self::PASSWORD);
    }

    /** يملأ الأنواع غير المسعّرة فقط؛ لا يكتب فوق أسعار مستوردة من OD-13. */
    private function seedPrices(): int
    {
        $count = 0;

        foreach ($this->categories as $name => $category) {
            [$floor, $ceiling] = self::PRICE_BANDS[$name] ?? [150, 1000];
            $types = $category->problemTypes()->where('is_other', false)->whereNull('employee_price_min')->orderBy('sort')->get();
            $step = $types->count() > 1 ? ($ceiling - $floor) / ($types->count() - 1) : 0;

            foreach ($types->values() as $index => $type) {
                $minimum = $this->roundTo50($floor + $step * $index * 0.6);
                $maximum = max($minimum + 100, $this->roundTo50($minimum * 2.5));

                $type->update([
                    'employee_price_min' => number_format($minimum, 2, '.', ''),
                    'employee_price_max' => number_format(min($maximum, $ceiling * 1.5), 2, '.', ''),
                    'employee_price_notes' => self::PRICE_NOTE,
                ]);
                $count++;
            }
        }

        return $count;
    }

    private function seedEmployees(): void
    {
        foreach (self::EMPLOYEES as $index => $employee) {
            $this->provider('employee'.($index + 1), $employee, EmploymentType::Employee, ProviderStatus::Active, [
                'available_now' => $index % 4 !== 3,
                'rating_avg' => 4.2 + ($index % 8) / 10,
                'ratings_count' => 5 + $index * 3,
                'completed_orders_count' => 8 + $index * 4,
            ]);
        }
    }

    private function seedIndependents(): void
    {
        foreach (self::INDEPENDENTS as $index => $provider) {
            $this->provider('provider'.($index + 1), $provider, EmploymentType::Independent, ProviderStatus::Active, [
                'rating_avg' => 4.5,
                'ratings_count' => 12,
                'completed_orders_count' => 20,
            ]);
        }
    }

    /** طلبا انضمام لتجربة المراجعة من اللوحة، وفني موقوف لتجربة إعادة التفعيل. */
    private function seedApplicants(): void
    {
        $applicants = [
            ['applicant1', ['name' => 'حسام الدين رمضان', 'categories' => ['سباكة'], 'years' => 3, 'bio' => 'طلب انضمام — الهاتف موثّق.'], now()],
            ['applicant2', ['name' => 'رامي عاطف', 'categories' => ['كهرباء'], 'years' => 2, 'bio' => 'طلب انضمام — الهاتف غير موثّق.'], null],
        ];

        foreach ($applicants as [$key, $data, $phoneVerifiedAt]) {
            // التقديم من التطبيق يضبطها مستقلًا، والمراجع يختار الصفة عند الاعتماد.
            $profile = $this->provider($key, $data, EmploymentType::Independent, ProviderStatus::PendingReview, [
                'phone_verified_at' => $phoneVerifiedAt,
                'reviewed_at' => null,
                'available_now' => false,
            ]);

            foreach (['ID_FRONT', 'ID_BACK'] as $type) {
                $path = "provider-documents/demo-{$key}-".strtolower($type).'.png';
                Storage::disk('local')->put($path, base64_decode(self::PLACEHOLDER_PNG));
                $profile->documents()->updateOrCreate(['type' => $type], ['path' => $path]);
            }
        }

        $this->provider('suspended1', ['name' => 'فادي لطفي', 'categories' => ['نجارة'], 'years' => 6, 'bio' => 'فني موقوف للتجربة.'],
            EmploymentType::Employee, ProviderStatus::Suspended, [
                'available_now' => false,
                'suspension_reason' => 'تأخير متكرر عن المواعيد (بيان تجريبي).',
            ]);
    }

    /**
     * @param  array{name: string, categories: list<string>, years: int, bio: string}  $data
     * @param  array<string, mixed>  $attributes
     */
    private function provider(string $key, array $data, EmploymentType $type, ProviderStatus $status, array $attributes = []): ProviderProfile
    {
        $user = $this->user("{$key}@bremo.test", $data['name']);

        $profile = ProviderProfile::query()->updateOrCreate(['user_id' => $user->id], [
            'status' => $status,
            'employment_type' => $type,
            'bio' => $data['bio'],
            'experience_years' => $data['years'],
            'available_now' => true,
            'phone_verified_at' => now(),
            'submitted_at' => now()->subDays(10),
            'reviewed_at' => now()->subDays(9),
            'payout_method' => PayoutMethod::Instapay,
            'payout_details' => $user->phone,
            ...$attributes,
        ]);

        $categoryIds = collect($data['categories'])->map(fn (string $name): int => $this->categories[$name]->id);
        $profile->categories()->sync($categoryIds);
        $profile->specialties()->sync(ProblemType::query()->whereIn('category_id', $categoryIds)->where('is_other', false)->pluck('id'));
        $profile->areas()->sync($this->areas->pluck('id'));

        return $profile;
    }

    /** @return Collection<int, User> */
    private function seedCustomers(): Collection
    {
        $customers = collect(self::CUSTOMERS)->map(function (string $name, int $index): User {
            $user = $this->user('customer'.($index + 1).'@bremo.test', $name);
            $this->address($user, $index);

            return $user;
        });

        // الحساب المستخدم سابقًا في تجارب الإشعارات والمحاكي.
        $push = $this->user('push-test@bremo.test', 'عميل تجربة الإشعارات', 'PushTest-2026');
        $this->address($push, 0);
        $customers->push($push);

        $blocked = $this->user('blocked1@bremo.test', 'عميل محظور');
        $blocked->update(['status' => UserStatus::Blocked, 'blocked_reason' => 'إساءة متكررة للفنيين (بيان تجريبي).']);

        return $customers;
    }

    private function address(User $user, int $index): CustomerAddress
    {
        $area = $this->areas[$index % $this->areas->count()];

        return CustomerAddress::query()->updateOrCreate(['user_id' => $user->id, 'label' => 'المنزل'], [
            'city_id' => $this->city->id,
            'area_id' => $area->id,
            'address_text' => $area->name.'، شارع '.(10 + $index).'، عمارة '.(3 + $index),
            'building' => (string) (3 + $index),
            'floor' => (string) (1 + $index % 6),
            'apartment' => (string) (2 + $index),
            'landmark' => 'بجوار المسجد',
            'lat' => number_format((float) $this->city->center_lat + ($index - 3) * 0.004, 7, '.', ''),
            'lng' => number_format((float) $this->city->center_lng + ($index - 3) * 0.003, 7, '.', ''),
            'is_default' => true,
        ]);
    }

    private function user(string $email, string $name, string $password = self::PASSWORD): User
    {
        static $phone = 1000000;
        $phone++;

        return User::query()->updateOrCreate(['email' => $email], [
            'name' => $name,
            'email_verified_at' => now(),
            'password' => $password,
            'phone' => '0100'.$phone,
            'status' => UserStatus::Active,
            'blocked_reason' => null,
            'accepted_terms_version' => 1,
            'terms_accepted_at' => now(),
        ]);
    }

    /**
     * طلبات مفتوحة تنتظر التعيين من اللوحة (وضع الموظفين).
     *
     * @param  Collection<int, User>  $customers
     */
    private function seedOpenOrders(Collection $customers): void
    {
        $requests = [
            ['سباكة', 'تسريب مياه', 'تسريب تحت حوض المطبخ والمياه بتنزل على الأرض.'],
            ['كهرباء', 'فصل القاطع باستمرار', 'القاطع بيفصل كل ما نشغل السخان والتكييف مع بعض.'],
            ['تكييفات', 'التكييف لا يبرد', 'التكييف شغال بس الهوا مش بارد خالص.'],
            ['أجهزة منزلية', 'صيانة غسالة', 'الغسالة مش بتعصر والمية بتفضل جواها.'],
        ];

        foreach ($requests as $index => [$categoryName, $problemName, $description]) {
            $customer = $customers[$index];

            if (Order::query()->where('customer_id', $customer->id)->exists()) {
                continue;
            }

            $category = $this->categories[$categoryName];
            $address = $customer->addresses()->firstOrFail();

            Order::factory()->create([
                'customer_id' => $customer->id,
                'customer_address_id' => $address->id,
                'city_id' => $this->city->id,
                'area_id' => $address->area_id,
                'category_id' => $category->id,
                'problem_type_id' => $category->problemTypes()->where('name', $problemName)->value('id'),
                'description' => $description,
                'address_text' => $address->address_text,
                'building' => $address->building,
                'floor' => $address->floor,
                'apartment' => $address->apartment,
                'lat' => $address->lat,
                'lng' => $address->lng,
                'selection_deadline_at' => now()->addHours(2),
            ]);
        }
    }

    private function roundTo50(float $value): int
    {
        return (int) (round($value / 50) * 50);
    }
}
