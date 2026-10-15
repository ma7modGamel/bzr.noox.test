<?php

declare(strict_types=1);

namespace App\Filament\Pages;

use App\Filament\Support\NavigationGroups;
use App\Filament\Support\SettlementActions;
use App\Modules\Settings\Enums\OperatingMode;
use App\Modules\Settlements\Services\CommissionSummary;
use BackedEnum;
use Filament\Forms\Components\DatePicker;
use Filament\Pages\Page;
use Filament\Schemas\Schema;
use Filament\Support\Icons\Heroicon;
use Illuminate\Support\Carbon;

/**
 * ملخص العمولات حسب الفترة — 16 §المال، DEC-066. للمدير العام وحده.
 *
 * @property-read Schema $form
 */
final class CommissionSummaryPage extends Page
{
    protected static string|BackedEnum|null $navigationIcon = Heroicon::OutlinedChartBar;

    protected static string|null|\UnitEnum $navigationGroup = NavigationGroups::Money;

    protected static ?int $navigationSort = 40;

    protected static ?string $slug = 'commission-summary';

    protected string $view = 'filament.pages.commission-summary';

    /** @var array{from?: string, to?: string} */
    public array $data = [];

    public static function getNavigationLabel(): string
    {
        return 'ملخص العمولات';
    }

    public function getTitle(): string
    {
        return 'ملخص العمولات';
    }

    public function getSubheading(): ?string
    {
        return 'الطلبات المغلقة في الفترة حسب تاريخ الإغلاق. في وضع الموظفين العمولة = المصنعية كاملة (BR-065).';
    }

    public static function canAccess(): bool
    {
        return SettlementActions::super();
    }

    public function mount(): void
    {
        $this->form->fill([
            'from' => now(config('app.display_timezone'))->startOfMonth()->toDateString(),
            'to' => now(config('app.display_timezone'))->toDateString(),
        ]);
    }

    public function form(Schema $schema): Schema
    {
        return $schema
            ->statePath('data')
            ->columns(2)
            ->components([
                DatePicker::make('from')->label('من')->required()->live(),
                DatePicker::make('to')->label('إلى')->required()->live(),
            ]);
    }

    /** @return list<array{label: string, row: array{orders: int, labor: string, materials: string, refunded: string, commission: string}}> */
    public function rows(): array
    {
        $zone = config('app.display_timezone');
        $from = Carbon::parse($this->data['from'] ?? now()->toDateString(), $zone)->startOfDay();
        $to = Carbon::parse($this->data['to'] ?? now()->toDateString(), $zone)->endOfDay();

        if ($to->lt($from)) {
            [$from, $to] = [$to->copy()->startOfDay(), $from->copy()->endOfDay()];
        }

        $summary = app(CommissionSummary::class)->between($from->utc(), $to->utc());

        return [
            ...array_map(fn (OperatingMode $mode): array => ['label' => $mode->getLabel(), 'row' => $summary[$mode->value]], OperatingMode::cases()),
            ['label' => 'الإجمالي', 'row' => $summary['TOTAL']],
        ];
    }
}
