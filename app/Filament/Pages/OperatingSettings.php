<?php

declare(strict_types=1);

namespace App\Filament\Pages;

use App\Filament\Support\NavigationGroups;
use App\Modules\Identity\Models\Admin;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Enums\CfgGroup;
use App\Modules\Settings\Enums\CfgType;
use App\Modules\Settings\Services\FeatureGate;
use App\Modules\Settings\Services\SettingsRepository;
use App\Support\Exceptions\DomainException;
use BackedEnum;
use Filament\Actions\Action;
use Filament\Forms\Components\KeyValue;
use Filament\Forms\Components\TextInput;
use Filament\Forms\Components\TimePicker;
use Filament\Forms\Components\Toggle;
use Filament\Notifications\Notification;
use Filament\Pages\Page;
use Filament\Schemas\Components\Section;
use Filament\Schemas\Components\Tabs;
use Filament\Schemas\Components\Tabs\Tab;
use Filament\Schemas\Components\Text;
use Filament\Schemas\Schema;
use Filament\Support\Icons\Heroicon;
use Illuminate\Contracts\Support\Htmlable;

/**
 * الإعدادات — 04 §الإعدادات. للمدير العام وحده (23).
 * أهمها مفتاحا المرحلة CFG-090/CFG-091 (39): تبديلهما يغيّر سلوك المنصة بلا نشر إصدار.
 *
 * @property-read Schema $form
 */
final class OperatingSettings extends Page
{
    protected static string|BackedEnum|null $navigationIcon = Heroicon::OutlinedAdjustmentsHorizontal;

    protected static string|null|\UnitEnum $navigationGroup = NavigationGroups::Configuration;

    protected static ?int $navigationSort = 90;

    protected string $view = 'filament.pages.operating-settings';

    /** @var array<string, mixed> */
    public array $data = [];

    public static function getNavigationLabel(): string
    {
        return 'الإعدادات';
    }

    public function getTitle(): string|Htmlable
    {
        return 'الإعدادات ومفاتيح المرحلة';
    }

    public function getSubheading(): ?string
    {
        return 'المرجع: 04 §الإعدادات و39-OPERATING-MODES. كل مفتاح يحمل معرّفه في الوثائق.';
    }

    public static function canAccess(): bool
    {
        /** @var Admin|null $admin */
        $admin = auth('admin')->user();

        return $admin?->isSuper() ?? false;
    }

    public function mount(): void
    {
        $settings = app(SettingsRepository::class);

        $this->data = collect(Cfg::cases())
            ->mapWithKeys(fn (Cfg $cfg): array => [$cfg->value => $settings->get($cfg)])
            ->all();
    }

    public function form(Schema $schema): Schema
    {
        return $schema
            ->statePath('data')
            ->components([
                Tabs::make()->tabs(
                    collect(CfgGroup::cases())
                        ->map(fn (CfgGroup $group): Tab => Tab::make($group->getLabel())
                            ->schema(self::groupComponents($group)))
                        ->all(),
                ),
            ]);
    }

    /** @return list<mixed> */
    private static function groupComponents(CfgGroup $group): array
    {
        $components = [];

        if ($group === CfgGroup::OperatingMode) {
            $components[] = Section::make('كيف يعمل هذان المفتاحان')
                ->schema([
                    Text::make(
                        'إطفاء «عروض السعر» يضع المنصة في وضع الموظفين: لا عروض ولا اختيار، '
                        .'ومدير التشغيل يعيّن كل طلب لموظف مؤهل، والمعاينة مجانية. '
                        .'التبديل يسري على الطلبات الجديدة فقط؛ القائمة تكمل بوضعها المثبت (BR-009).'
                    ),
                ]);
        }

        foreach (Cfg::inGroup($group) as $cfg) {
            $components[] = self::field($cfg);
        }

        return $components;
    }

    private static function field(Cfg $cfg): mixed
    {
        $label = $cfg->label();
        $hint = $cfg->id();

        return match ($cfg->type()) {
            CfgType::Bool => Toggle::make($cfg->value)->label($label)->hint($hint)->inline(false),
            CfgType::Time => TimePicker::make($cfg->value)->label($label)->hint($hint)->seconds(false),
            CfgType::Json => KeyValue::make($cfg->value)->label($label)->hint($hint)
                ->keyLabel('من')->valueLabel('إلى'),
            CfgType::Decimal => TextInput::make($cfg->value)->label($label)->hint($hint)
                ->numeric()->step('0.0001')
                ->placeholder('غير محدد — قرار مفتوح'),
            CfgType::Int => TextInput::make($cfg->value)->label($label)->hint($hint)->numeric()->integer(),
            CfgType::String => TextInput::make($cfg->value)->label($label)->hint($hint),
        };
    }

    protected function getFormActions(): array
    {
        return [
            Action::make('save')
                ->label('حفظ الإعدادات')
                ->submit('save'),
        ];
    }

    public function save(): void
    {
        /** @var Admin $admin */
        $admin = auth('admin')->user();
        $features = app(FeatureGate::class);

        $values = $this->form->getState();

        try {
            // BR-009 — لا يُفعَّل CFG-091 إلا مع CFG-090
            $features->assertFlagCombination(
                (bool) ($values[Cfg::OffersEnabled->value] ?? false),
                (bool) ($values[Cfg::InspectionFeeEnabled->value] ?? false),
            );
        } catch (DomainException $e) {
            Notification::make()->danger()->title('إعداد مرفوض')->body($e->getMessage())->send();

            return;
        }

        app(SettingsRepository::class)->setMany($values, $admin->getKey());

        Notification::make()
            ->success()
            ->title('تم حفظ الإعدادات')
            ->body('وضع التشغيل الحالي: '.app(FeatureGate::class)->mode()->getLabel())
            ->send();
    }
}
