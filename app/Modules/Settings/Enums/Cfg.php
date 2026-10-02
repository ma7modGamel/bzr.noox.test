<?php

declare(strict_types=1);

namespace App\Modules\Settings\Enums;

/**
 * كل إعداد قابل للتغيير من لوحة الإدارة — المرجع: 04-BUSINESS-RULES §الإعدادات.
 * القيمة = مفتاح الصف في جدول `settings`. المعرّف (CFG-0xx) يبقى مرئيًا في الواجهة
 * ليظل الربط بالوثائق مباشرًا.
 */
enum Cfg: string
{
    // ── وضع التشغيل (39، DEC-039، DEC-040) ─────────────────────────────
    case OffersEnabled = 'operating.offers_enabled';                        // CFG-090
    case InspectionFeeEnabled = 'operating.inspection_fee_enabled';         // CFG-091

    // ── العروض (DEC-003) ───────────────────────────────────────────────
    case OffersWindowNowMinutes = 'offers.window_now_minutes';              // CFG-010
    case OffersWindowScheduledLeadMinutes = 'offers.window_scheduled_lead_minutes'; // CFG-010b
    case OffersWindowScheduledMaxHours = 'offers.window_scheduled_max_hours';       // CFG-010b
    case MaxOffersPerOrder = 'offers.max_per_order';                        // CFG-011
    case MinOfferAmount = 'offers.min_amount';                              // CFG-012
    case SelectionGraceNowMinutes = 'offers.selection_grace_now_minutes';   // CFG-013
    case SelectionLeadScheduledMinutes = 'offers.selection_lead_scheduled_minutes'; // CFG-013b

    // ── المواعيد والجدولة (BR-017) ─────────────────────────────────────
    case ServiceHoursFrom = 'scheduling.service_hours_from';                // CFG-020
    case ServiceHoursTo = 'scheduling.service_hours_to';                    // CFG-020
    case ScheduledSlots = 'scheduling.slots';                               // CFG-021
    case MinLeadMinutesBeforeSlot = 'scheduling.min_lead_minutes';          // CFG-022
    case MaxOpenOrdersPerCustomer = 'scheduling.max_open_orders';           // CFG-030

    // ── التعيين — وضع الموظفين (BR-007) ────────────────────────────────
    case AssignmentAlertNowMinutes = 'assignment.alert_now_minutes';                 // CFG-092
    case AssignmentAlertScheduledLeadMinutes = 'assignment.alert_scheduled_lead_minutes'; // CFG-092
    case AssignmentDeadlineNowMinutes = 'assignment.deadline_now_minutes';           // CFG-093

    // ── التنفيذ والتتبع ────────────────────────────────────────────────
    case TripStartReminderMinutes = 'execution.trip_start_reminder_minutes';   // CFG-031
    case TripStartAlertMinutes = 'execution.trip_start_alert_minutes';         // CFG-032
    case ArrivalLateAlertMinutes = 'execution.arrival_late_alert_minutes';     // CFG-032b
    case CustomerNoShowWaitMinutes = 'execution.customer_no_show_wait_minutes'; // CFG-033

    // ── الأسعار والمقترحات ─────────────────────────────────────────────
    case ProposalTimeoutMinutes = 'pricing.proposal_timeout_minutes';       // CFG-040

    // ── الدفع ──────────────────────────────────────────────────────────
    case PaymentDelayAlertHours = 'payments.delay_alert_hours';             // CFG-050
    case AutoCloseHours = 'payments.auto_close_hours';                      // CFG-051
    case FawryCodeValidityHours = 'payments.fawry_code_validity_hours';     // CFG-052
    case ChannelCashEnabled = 'payments.channel_cash_enabled';              // CFG-053 (DEC-050)
    case ChannelInstapayEnabled = 'payments.channel_instapay_enabled';      // CFG-054
    case ChannelFawryEnabled = 'payments.channel_fawry_enabled';            // CFG-055 (OD-10)
    case InstapayAddress = 'payments.instapay_address';                     // CFG-056
    case InstapayDisplayName = 'payments.instapay_display_name';            // CFG-057
    case InstapayLink = 'payments.instapay_link';                           // CFG-058

    // ── العمولة والتسويات ──────────────────────────────────────────────
    case DisputeWindowHours = 'settlements.dispute_window_hours';           // CFG-060
    case ProviderDebtLimit = 'settlements.provider_debt_limit';             // CFG-061 (OD-02)
    case DefaultCommissionRate = 'settlements.default_commission_rate';     // CFG-062 (OD-01)
    case PayoutCycle = 'settlements.payout_cycle';                          // CFG-063
    case EmployeeRemittanceCycle = 'settlements.employee_remittance_cycle'; // CFG-094

    // ── التقييم والتواصل ───────────────────────────────────────────────
    case RatingWindowDays = 'ratings.window_days';                          // CFG-070
    case PhoneHideAfterCloseHours = 'ratings.phone_hide_after_close_hours'; // CFG-071
    case RatingReminderHours = 'ratings.reminder_hours';                    // CFG-072 (DEC-058)

    // ── الموقع والخصوصية (DEC-030) ─────────────────────────────────────
    case LocationUpdateSeconds = 'tracking.location_update_seconds';        // CFG-080
    case EtaRecalcSeconds = 'tracking.eta_recalc_seconds';                  // CFG-081
    case EtaRecalcDistanceMeters = 'tracking.eta_recalc_distance_m';        // CFG-081
    case ArrivalWarningDistanceMeters = 'tracking.arrival_warning_distance_m'; // CFG-082
    case ArrivalPointRetentionDays = 'tracking.arrival_point_retention_days';   // CFG-083

    /** معرّف الإعداد في الوثائق. */
    public function id(): string
    {
        return match ($this) {
            self::OffersWindowNowMinutes => 'CFG-010',
            self::OffersWindowScheduledLeadMinutes, self::OffersWindowScheduledMaxHours => 'CFG-010b',
            self::MaxOffersPerOrder => 'CFG-011',
            self::MinOfferAmount => 'CFG-012',
            self::SelectionGraceNowMinutes => 'CFG-013',
            self::SelectionLeadScheduledMinutes => 'CFG-013b',
            self::ServiceHoursFrom, self::ServiceHoursTo => 'CFG-020',
            self::ScheduledSlots => 'CFG-021',
            self::MinLeadMinutesBeforeSlot => 'CFG-022',
            self::MaxOpenOrdersPerCustomer => 'CFG-030',
            self::TripStartReminderMinutes => 'CFG-031',
            self::TripStartAlertMinutes => 'CFG-032',
            self::ArrivalLateAlertMinutes => 'CFG-032b',
            self::CustomerNoShowWaitMinutes => 'CFG-033',
            self::ProposalTimeoutMinutes => 'CFG-040',
            self::PaymentDelayAlertHours => 'CFG-050',
            self::AutoCloseHours => 'CFG-051',
            self::FawryCodeValidityHours => 'CFG-052',
            self::ChannelCashEnabled => 'CFG-053',
            self::ChannelInstapayEnabled => 'CFG-054',
            self::ChannelFawryEnabled => 'CFG-055',
            self::InstapayAddress => 'CFG-056',
            self::InstapayDisplayName => 'CFG-057',
            self::InstapayLink => 'CFG-058',
            self::DisputeWindowHours => 'CFG-060',
            self::ProviderDebtLimit => 'CFG-061',
            self::DefaultCommissionRate => 'CFG-062',
            self::PayoutCycle => 'CFG-063',
            self::RatingWindowDays => 'CFG-070',
            self::PhoneHideAfterCloseHours => 'CFG-071',
            self::RatingReminderHours => 'CFG-072',
            self::LocationUpdateSeconds => 'CFG-080',
            self::EtaRecalcSeconds, self::EtaRecalcDistanceMeters => 'CFG-081',
            self::ArrivalWarningDistanceMeters => 'CFG-082',
            self::ArrivalPointRetentionDays => 'CFG-083',
            self::OffersEnabled => 'CFG-090',
            self::InspectionFeeEnabled => 'CFG-091',
            self::AssignmentAlertNowMinutes, self::AssignmentAlertScheduledLeadMinutes => 'CFG-092',
            self::AssignmentDeadlineNowMinutes => 'CFG-093',
            self::EmployeeRemittanceCycle => 'CFG-094',
        };
    }

    public function label(): string
    {
        return match ($this) {
            self::OffersEnabled => 'تفعيل عروض السعر',
            self::InspectionFeeEnabled => 'تفعيل رسوم المعاينة',
            self::OffersWindowNowMinutes => 'نافذة العروض لطلب الآن (دقيقة)',
            self::OffersWindowScheduledLeadMinutes => 'إغلاق عروض المجدول قبل بداية الفترة بـ (دقيقة)',
            self::OffersWindowScheduledMaxHours => 'أقصى مدة لنافذة عروض المجدول (ساعة)',
            self::MaxOffersPerOrder => 'أقصى عدد عروض للطلب',
            self::MinOfferAmount => 'أقل مبلغ عرض (جنيه)',
            self::SelectionGraceNowMinutes => 'مهلة الاختيار بعد نهاية النافذة — الآن (دقيقة)',
            self::SelectionLeadScheduledMinutes => 'مهلة الاختيار قبل بداية الفترة — مجدول (دقيقة)',
            self::ServiceHoursFrom => 'بداية ساعات الخدمة لطلبات الآن',
            self::ServiceHoursTo => 'نهاية ساعات الخدمة لطلبات الآن',
            self::ScheduledSlots => 'الفترات المجدولة',
            self::MinLeadMinutesBeforeSlot => 'أقل مدة قبل بداية الفترة عند النشر (دقيقة)',
            self::MaxOpenOrdersPerCustomer => 'أقصى طلبات مفتوحة للعميل',
            self::AssignmentAlertNowMinutes => 'تنبيه طلب الآن بلا تعيين بعد (دقيقة)',
            self::AssignmentAlertScheduledLeadMinutes => 'تنبيه المجدول بلا تعيين قبل الفترة بـ (دقيقة)',
            self::AssignmentDeadlineNowMinutes => 'انتهاء طلب الآن بلا تعيين بعد (دقيقة)',
            self::TripStartReminderMinutes => 'تذكير الفني ببدء التحرك بعد (دقيقة)',
            self::TripStartAlertMinutes => 'تنبيه الإدارة لعدم بدء التحرك بعد (دقيقة)',
            self::ArrivalLateAlertMinutes => 'تنبيه التأخر في الوصول بعد الوقت المتوقع بـ (دقيقة)',
            self::CustomerNoShowWaitMinutes => 'انتظار العميل غير الموجود (دقيقة)',
            self::ProposalTimeoutMinutes => 'مهلة مقترح السعر (دقيقة)',
            self::PaymentDelayAlertHours => 'تنبيه تأخر الدفع (ساعة)',
            self::AutoCloseHours => 'الإغلاق التلقائي بعد الإنهاء (ساعة)',
            self::FawryCodeValidityHours => 'صلاحية كود دفع فوري (ساعة)',
            self::ChannelCashEnabled => 'قناة الدفع النقدي',
            self::ChannelInstapayEnabled => 'قناة تحويل إنستاباي (بتأكيد الإدارة)',
            self::ChannelFawryEnabled => 'قناة فوري',
            self::InstapayAddress => 'عنوان إنستاباي للمنصة (IPA)',
            self::InstapayDisplayName => 'الاسم الظاهر لحساب إنستاباي',
            self::InstapayLink => 'رابط الدفع عبر إنستاباي (اختياري)',
            self::DisputeWindowHours => 'مهلة النزاع بعد الإغلاق (ساعة)',
            self::ProviderDebtLimit => 'حد دين الفني للمنع من العروض (جنيه)',
            self::DefaultCommissionRate => 'نسبة العمولة الافتراضية',
            self::PayoutCycle => 'دورية التحويل للفنيين',
            self::EmployeeRemittanceCycle => 'دورية توريد النقدية من الموظف',
            self::RatingWindowDays => 'نافذة التقييم (يوم)',
            self::PhoneHideAfterCloseHours => 'إخفاء الرقم بعد الإغلاق (ساعة)',
            self::RatingReminderHours => 'تذكير التقييم بعد الإغلاق (ساعة)',
            self::LocationUpdateSeconds => 'تحديث موقع الفني (ثانية)',
            self::EtaRecalcSeconds => 'إعادة حساب وقت الوصول (ثانية)',
            self::EtaRecalcDistanceMeters => 'إعادة حساب وقت الوصول عند تحرك (متر)',
            self::ArrivalWarningDistanceMeters => 'مسافة تحذير الوصول (متر)',
            self::ArrivalPointRetentionDays => 'حفظ نقطة الوصول (يوم)',
        };
    }

    public function type(): CfgType
    {
        return match ($this) {
            self::OffersEnabled, self::InspectionFeeEnabled,
            self::ChannelCashEnabled, self::ChannelInstapayEnabled, self::ChannelFawryEnabled => CfgType::Bool,
            self::InstapayAddress, self::InstapayDisplayName, self::InstapayLink => CfgType::String,
            self::MinOfferAmount, self::ProviderDebtLimit => CfgType::Decimal,
            self::DefaultCommissionRate => CfgType::Decimal,
            self::ServiceHoursFrom, self::ServiceHoursTo => CfgType::Time,
            self::ScheduledSlots => CfgType::Json,
            self::PayoutCycle, self::EmployeeRemittanceCycle => CfgType::String,
            default => CfgType::Int,
        };
    }

    public function group(): CfgGroup
    {
        return match ($this) {
            self::OffersEnabled, self::InspectionFeeEnabled => CfgGroup::OperatingMode,
            self::OffersWindowNowMinutes, self::OffersWindowScheduledLeadMinutes,
            self::OffersWindowScheduledMaxHours, self::MaxOffersPerOrder,
            self::MinOfferAmount, self::SelectionGraceNowMinutes,
            self::SelectionLeadScheduledMinutes => CfgGroup::Offers,
            self::ServiceHoursFrom, self::ServiceHoursTo, self::ScheduledSlots,
            self::MinLeadMinutesBeforeSlot, self::MaxOpenOrdersPerCustomer => CfgGroup::Scheduling,
            self::AssignmentAlertNowMinutes, self::AssignmentAlertScheduledLeadMinutes,
            self::AssignmentDeadlineNowMinutes => CfgGroup::Assignment,
            self::TripStartReminderMinutes, self::TripStartAlertMinutes,
            self::ArrivalLateAlertMinutes, self::CustomerNoShowWaitMinutes => CfgGroup::Execution,
            self::ProposalTimeoutMinutes => CfgGroup::Pricing,
            self::PaymentDelayAlertHours, self::AutoCloseHours,
            self::FawryCodeValidityHours, self::ChannelCashEnabled, self::ChannelInstapayEnabled,
            self::ChannelFawryEnabled, self::InstapayAddress, self::InstapayDisplayName,
            self::InstapayLink => CfgGroup::Payments,
            self::DisputeWindowHours, self::ProviderDebtLimit, self::DefaultCommissionRate,
            self::PayoutCycle, self::EmployeeRemittanceCycle => CfgGroup::Settlements,
            self::RatingWindowDays, self::PhoneHideAfterCloseHours, self::RatingReminderHours => CfgGroup::Ratings,
            self::LocationUpdateSeconds, self::EtaRecalcSeconds, self::EtaRecalcDistanceMeters,
            self::ArrivalWarningDistanceMeters, self::ArrivalPointRetentionDays => CfgGroup::Tracking,
        };
    }

    /** القيمة الافتراضية عند التهيئة الأولى. `null` = قرار مفتوح يجب حسمه (OD-01، OD-02). */
    public function default(): mixed
    {
        return match ($this) {
            self::OffersEnabled, self::InspectionFeeEnabled => false,
            self::OffersWindowNowMinutes => 30,
            self::OffersWindowScheduledLeadMinutes => 60,
            self::OffersWindowScheduledMaxHours => 24,
            self::MaxOffersPerOrder => 10,
            self::MinOfferAmount => '50.00',
            self::SelectionGraceNowMinutes => 15,
            self::SelectionLeadScheduledMinutes => 30,
            self::ServiceHoursFrom => '08:00',
            self::ServiceHoursTo => '22:00',
            self::ScheduledSlots => [
                ['from' => '09:00', 'to' => '11:00'],
                ['from' => '11:00', 'to' => '13:00'],
                ['from' => '13:00', 'to' => '15:00'],
                ['from' => '15:00', 'to' => '17:00'],
                ['from' => '17:00', 'to' => '19:00'],
                ['from' => '19:00', 'to' => '21:00'],
            ],
            self::MinLeadMinutesBeforeSlot => 90,
            self::MaxOpenOrdersPerCustomer => 3,
            self::AssignmentAlertNowMinutes => 15,
            self::AssignmentAlertScheduledLeadMinutes => 60,
            self::AssignmentDeadlineNowMinutes => 60,
            self::TripStartReminderMinutes => 10,
            self::TripStartAlertMinutes => 20,
            self::ArrivalLateAlertMinutes => 30,
            self::CustomerNoShowWaitMinutes => 20,
            self::ProposalTimeoutMinutes => 60,
            self::PaymentDelayAlertHours => 24,
            self::AutoCloseHours => 24,
            self::FawryCodeValidityHours => 24,
            self::ChannelCashEnabled, self::ChannelInstapayEnabled => true,
            self::ChannelFawryEnabled => false,     // OD-10 — حتى يجهز حساب فوري
            self::InstapayAddress, self::InstapayDisplayName, self::InstapayLink => null, // يُدخلها المدير العام
            self::DisputeWindowHours => 72,
            self::ProviderDebtLimit => null,       // OD-02 — قبل تفعيل CFG-090
            self::DefaultCommissionRate => null,   // OD-01 — قبل تفعيل CFG-090
            self::PayoutCycle => 'weekly',
            self::EmployeeRemittanceCycle => 'daily',
            self::RatingWindowDays => 7,
            self::PhoneHideAfterCloseHours => 24,
            self::RatingReminderHours => 24,
            self::LocationUpdateSeconds => 30,
            self::EtaRecalcSeconds => 120,
            self::EtaRecalcDistanceMeters => 300,
            self::ArrivalWarningDistanceMeters => 500,
            self::ArrivalPointRetentionDays => 90,
        };
    }

    /** إعداد لا يظهر إلا للمدير العام (23). */
    public function isSuperAdminOnly(): bool
    {
        return in_array($this->group(), [CfgGroup::OperatingMode, CfgGroup::Settlements], true)
            || in_array($this, [
                self::ChannelCashEnabled, self::ChannelInstapayEnabled, self::ChannelFawryEnabled,
                self::InstapayAddress, self::InstapayDisplayName, self::InstapayLink,
            ], true);
    }

    /** @return list<self> */
    public static function inGroup(CfgGroup $group): array
    {
        return array_values(array_filter(self::cases(), fn (self $c) => $c->group() === $group));
    }
}
