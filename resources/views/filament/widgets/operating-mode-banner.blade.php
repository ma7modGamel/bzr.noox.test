@php
    $mode = $this->getMode();
    $isEmployee = $mode === \App\Modules\Settings\Enums\OperatingMode::Employee;
@endphp

<x-filament-widgets::widget>
    <div class="bzr-info-strip flex flex-wrap items-center gap-x-4 gap-y-2">
        <span class="bzr-mode-badge" data-mode="{{ $mode->value }}">
            {{ $mode->getLabel() }}
        </span>

        <span class="text-sm">
            @if ($isEmployee)
                لا عروض سعر: كل طلب يُعيَّن يدويًا لمقدم خدمة مؤهل.
            @else
                العروض مفعّلة: الطلب يُبث للمؤهلين والعميل يختار.
            @endif

            @if ($this->isInspectionFree())
                المعاينة <strong>مجانية</strong>، والسعر يُعرض على العميل بعد الكشف.
            @else
                رسوم المعاينة مفعّلة من عروض مقدمي الخدمة.
            @endif
        </span>

        <span class="bzr-doc-ref ms-auto">CFG-090 · CFG-091 · 39-OPERATING-MODES</span>
    </div>
</x-filament-widgets::widget>
