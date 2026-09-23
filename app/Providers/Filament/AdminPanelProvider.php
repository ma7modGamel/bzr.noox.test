<?php

declare(strict_types=1);

namespace App\Providers\Filament;

use App\Filament\Support\NavigationGroups;
use Filament\Enums\ThemeMode;
use Filament\Http\Middleware\Authenticate;
use Filament\Http\Middleware\AuthenticateSession;
use Filament\Http\Middleware\DisableBladeIconComponents;
use Filament\Http\Middleware\DispatchServingFilamentEvent;
use Filament\Pages\Dashboard;
use Filament\Panel;
use Filament\PanelProvider;
use Filament\Support\Colors\Color;
use Filament\Support\Enums\Width;
use Illuminate\Cookie\Middleware\AddQueuedCookiesToResponse;
use Illuminate\Cookie\Middleware\EncryptCookies;
use Illuminate\Foundation\Http\Middleware\PreventRequestForgery;
use Illuminate\Routing\Middleware\SubstituteBindings;
use Illuminate\Session\Middleware\StartSession;
use Illuminate\View\Middleware\ShareErrorsFromSession;

/**
 * لوحة التشغيل — 16-ADMIN-OPERATIONS. ليست ERP: كل شاشة تخدم قرارًا تشغيليًا.
 * الهوية البصرية من 38-DESIGN-SYSTEM، والعربية RTL هي الأصل (DEC-035).
 */
final class AdminPanelProvider extends PanelProvider
{
    public function panel(Panel $panel): Panel
    {
        return $panel
            ->default()
            ->id('admin')
            ->path('admin')
            ->authGuard('admin')          // حسابات منفصلة عن المستخدمين (03)
            ->login()
            ->brandName('بازار — لوحة التشغيل')
            ->colors([
                'primary' => Color::hex('#149C94'), // primary-600 (38 §1)
                'info' => Color::hex('#00ACB6'),    // primary-500
                'warning' => Color::hex('#F0AC2C'), // star — التنبيه والتقييم
                'danger' => Color::hex('#C64F53'),  // الإلغاء فقط
                'success' => Color::hex('#1E8C8C'), // primary-700
                'gray' => Color::Slate,
            ])
            ->font('Cairo')                 // 38 §2 (OD-07)
            ->viteTheme('resources/css/filament/admin/theme.css')
            ->defaultThemeMode(ThemeMode::Light)
            ->maxContentWidth(Width::Full)
            ->sidebarCollapsibleOnDesktop()
            ->navigationGroups(NavigationGroups::all())
            ->databaseNotifications()
            ->spa()
            ->discoverResources(in: app_path('Filament/Resources'), for: 'App\Filament\Resources')
            ->discoverPages(in: app_path('Filament/Pages'), for: 'App\Filament\Pages')
            ->pages([
                Dashboard::class,
            ])
            ->discoverWidgets(in: app_path('Filament/Widgets'), for: 'App\Filament\Widgets')
            ->middleware([
                EncryptCookies::class,
                AddQueuedCookiesToResponse::class,
                StartSession::class,
                AuthenticateSession::class,
                ShareErrorsFromSession::class,
                PreventRequestForgery::class,
                SubstituteBindings::class,
                DisableBladeIconComponents::class,
                DispatchServingFilamentEvent::class,
            ])
            ->authMiddleware([
                Authenticate::class,
            ]);
    }
}
