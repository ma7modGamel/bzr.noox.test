<x-filament-panels::page>
    {{ $this->form }}

    <x-filament::section>
        <div style="overflow-x: auto;">
            <table style="width: 100%; border-collapse: collapse; font-size: 0.9rem;">
                <thead>
                    <tr style="border-bottom: 1px solid rgba(127, 127, 127, 0.25);">
                        @foreach (['الوضع', 'الطلبات المغلقة', 'المصنعية', 'الخامات', 'المسترد', 'العمولة'] as $heading)
                            <th style="padding: 0.6rem 0.75rem; text-align: start; font-weight: 600; white-space: nowrap;">{{ $heading }}</th>
                        @endforeach
                    </tr>
                </thead>
                <tbody>
                    @foreach ($this->rows() as $line)
                        <tr style="border-bottom: 1px solid rgba(127, 127, 127, 0.12); {{ $loop->last ? 'font-weight: 700;' : '' }}">
                            <td style="padding: 0.6rem 0.75rem; white-space: nowrap;">{{ $line['label'] }}</td>
                            <td style="padding: 0.6rem 0.75rem;">{{ $line['row']['orders'] }}</td>
                            <td style="padding: 0.6rem 0.75rem;">{{ $line['row']['labor'] }}</td>
                            <td style="padding: 0.6rem 0.75rem;">{{ $line['row']['materials'] }}</td>
                            <td style="padding: 0.6rem 0.75rem;">{{ $line['row']['refunded'] }}</td>
                            <td style="padding: 0.6rem 0.75rem;">{{ $line['row']['commission'] }}</td>
                        </tr>
                    @endforeach
                </tbody>
            </table>
        </div>
    </x-filament::section>
</x-filament-panels::page>
