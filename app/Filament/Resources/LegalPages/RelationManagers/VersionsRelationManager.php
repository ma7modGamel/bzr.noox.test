<?php

declare(strict_types=1);

namespace App\Filament\Resources\LegalPages\RelationManagers;

use App\Modules\Content\Actions\PublishLegalPageVersionAction;
use App\Modules\Content\Enums\LegalPageSlug;
use App\Modules\Content\Enums\LegalPageStatus;
use App\Modules\Content\Models\LegalPage;
use App\Modules\Content\Models\LegalPageVersion;
use Filament\Actions\Action;
use Filament\Actions\CreateAction;
use Filament\Actions\EditAction;
use Filament\Forms\Components\DateTimePicker;
use Filament\Forms\Components\RichEditor;
use Filament\Forms\Components\TextInput;
use Filament\Notifications\Notification;
use Filament\Resources\RelationManagers\RelationManager;
use Filament\Schemas\Components\Text;
use Filament\Schemas\Schema;
use Filament\Tables\Columns\IconColumn;
use Filament\Tables\Columns\TextColumn;
use Filament\Tables\Table;

/** سجل نسخ الصفحة: إنشاء مسودة، تعديلها، ونشرها (DEC-051). المنشور لا يُعدَّل؛ التغيير نسخة جديدة. */
final class VersionsRelationManager extends RelationManager
{
    protected static string $relationship = 'versions';

    protected static ?string $title = 'النسخ';

    public function form(Schema $schema): Schema
    {
        return $schema->components([
            TextInput::make('title')->label('العنوان')->required()->maxLength(150),
            DateTimePicker::make('effective_at')
                ->label('تاريخ السريان')
                ->helperText('فارغ = من لحظة النشر.')
                ->seconds(false),
            RichEditor::make('body')
                ->label('المحتوى')
                ->required()
                ->toolbarButtons(['bold', 'italic', 'underline', 'h2', 'h3', 'bulletList', 'orderedList', 'link', 'blockquote', 'undo', 'redo'])
                ->columnSpanFull(),
        ]);
    }

    public function table(Table $table): Table
    {
        return $table
            ->defaultSort('version', 'desc')
            ->columns([
                TextColumn::make('version')->label('النسخة'),
                TextColumn::make('title')->label('العنوان'),
                TextColumn::make('status')->label('الحالة')->badge(),
                IconColumn::make('requires_legal_review')->label('تحتاج مراجعة قانونية')->boolean()
                    ->trueColor('warning')->falseColor('gray'),
                TextColumn::make('effective_at')->label('السريان')->dateTime('Y-m-d H:i'),
                TextColumn::make('terms_version')->label('نسخة الشروط (BR-018)')->placeholder('—'),
                TextColumn::make('publisher.name')->label('نشرها')->placeholder('—'),
            ])
            ->headerActions([
                CreateAction::make()
                    ->label('نسخة جديدة (مسودة)')
                    ->mutateDataUsing(function (array $data): array {
                        /** @var LegalPage $page */
                        $page = $this->getOwnerRecord();
                        $data['version'] = (int) $page->versions()->max('version') + 1;
                        $data['status'] = LegalPageStatus::Draft;
                        $data['requires_legal_review'] = true;
                        $data['created_by'] = auth('admin')->id();

                        return $data;
                    }),
            ])
            ->recordActions([
                EditAction::make()->visible(fn (LegalPageVersion $record): bool => $record->status === LegalPageStatus::Draft),
                Action::make('publish')
                    ->label('نشر')
                    ->color('success')
                    ->visible(fn (LegalPageVersion $record): bool => $record->status === LegalPageStatus::Draft)
                    ->requiresConfirmation()
                    ->modalHeading('نشر النسخة')
                    ->schema(fn (LegalPageVersion $record): array => [
                        Text::make($record->page->slug === LegalPageSlug::Terms
                            ? 'نشر الشروط يُنشئ نسخة شروط جديدة: كل عميل وافق على نسخة أقدم سيُطلب منه الموافقة عند أول طلب بعد تاريخ السريان (BR-018). تأكد من اكتمال المراجعة القانونية (OD-04).'
                            : 'ستظهر هذه النسخة في التطبيق وعلى الصفحة العامة من تاريخ السريان. تأكد من اكتمال المراجعة القانونية (OD-04).'),
                    ])
                    ->action(function (LegalPageVersion $record): void {
                        app(PublishLegalPageVersionAction::class)->execute($record, auth('admin')->user());
                        Notification::make()->title('تم النشر')->success()->send();
                    }),
            ]);
    }
}
