# 26 — ربط الشاشات بالمسارات

الروابط أدناه تصف وضع السوق. في وضع الموظفين تسقط شاشات العروض (C06/C07/C08، P09/P10/P11) ويحل محلها SCR-C36 للعميل وSCR-A15 للإدارة (39).

## العميل
| الشاشة | حالة الطلب | الإجراءات (API) |
|---|---|---|
| SCR-C03..C05 | — | رفع وسائط، `publishRequest` (T-01) |
| SCR-C06 | OPEN | `acceptOffer` (من C08)، `startConversation`، `cancelOrder` (T-04)، `updateRequest`، `republish` |
| SCR-C07 | OPEN | عرض ملف الفني، الانتقال إلى C08، محادثة، بلاغ (C28) |
| SCR-C08 | OPEN | `acceptOffer` + طريقة الدفع (T-02) |
| SCR-C09 | CONFIRMED، ON_THE_WAY، ARRIVED، IN_PROGRESS | تتبع، اتصال، محادثة، `createShareLink`، `cancelOrder` (T-08/09)، `changePaymentMethod`، `openDispute` (بعد الوصول) |
| SCR-C30 | AWAITING_QUOTE_APPROVAL | `approveProposal` (T-14)، `rejectProposal` (T-15) |
| SCR-C31 | IN_PROGRESS + مقترح معلّق | `approveProposal`، `rejectProposal` |
| SCR-C21، C22 | AWAITING_PAYMENT | `createPayment` (إلكتروني)، `changePaymentMethod`، `openDispute` |
| SCR-C23 | AWAITING_CONFIRMATION | `confirmCompletion` (T-21)، `openDispute` (T-22) |
| SCR-C24 | CLOSED | `rateProvider` |
| SCR-C26، C27 | CLOSED (≤ 72 ساعة) | `openDispute` بعد الإغلاق |
| SCR-C18، C19 | OPEN وما بعدها | `sendMessage` |
| SCR-C36 *(موظفين)* | OPEN | متابعة الانتظار، `updateRequest`، `cancelOrder` (T-04) |

## الفني
| الشاشة | حالة الطلب | الإجراءات |
|---|---|---|
| SCR-P02..P07 | — | `submitProviderApplication`، `resubmitProviderApplication` |
| SCR-P08 | — | `setAvailability`، قائمة الطلبات المتاحة |
| SCR-P09، P10 | OPEN | `submitOffer` (O-01) |
| SCR-P11 | OPEN | `withdrawOffer` (O-02) |
| SCR-P12 | CONFIRMED | `startTrip` (T-05)، `backOut` (T-06) |
| SCR-P13 | ON_THE_WAY | `sendLocation`، `markArrived` (T-10)، `backOut` (T-07) |
| SCR-P14 | ARRIVED | `startWork` (T-11)، `submitProposal(EXECUTION_QUOTE)` (T-12، مع دليل السعر وسبب الخروج في وضع الموظفين — BR-046)، `completeInspectionOnly` (T-13)، `reportUnableToPerform` / `reportCustomerNoShow` (T-16)، `openDispute` |
| SCR-P15، P20 | IN_PROGRESS | `submitProposal`، `withdrawProposal`، `completeWork` (T-17)، `reportUnableToPerform` (T-18) |
| SCR-P16 | AWAITING_PAYMENT | `confirmCashReceived` (T-19)، `openDispute` |
| SCR-P17 | CLOSED | `rateCustomer` |
| SCR-P18 | — | كشف المستحقات (قراءة) |

## الإدارة
| الشاشة | الإجراءات |
|---|---|
| SCR-A03 | `adminCancel` (T-26)، `adminReopen` (T-06/07)، `adminRecordCash` (T-19)، `adminCloseWithoutPayment` (T-25)، ملاحظة |
| SCR-A07 | `resolveDispute` (T-23/24)، `recordRefund` |
| SCR-A04، A05 | `approveProvider`، `rejectProvider`، `markPhoneVerified`، `suspendProvider`، `reactivateProvider` |
| SCR-A12 | `recordRemittance`، `recordPayout` |
| SCR-A15 *(موظفين)* | `assignProvider` (T-27)، `reassignProvider` (T-29) |

كل حالة طلب لها شاشة واحدة على الأقل لكل طرف له إجراء فيها؛ لا حالة بلا واجهة (راجع 37).
