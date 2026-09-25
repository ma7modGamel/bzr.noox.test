package noox.bzr.design

import androidx.annotation.DrawableRes

// Component states shared with BzrComponents.swift (43 §3): same names, same cases.

enum class ButtonVisualState { Normal, Pressed, Disabled, Loading }
enum class SelectionState { Selected, Unselected, Disabled }
enum class FieldVisualState { Empty, Filled, Focused, Error, Disabled }
enum class OfferVariant { Execution, Inspection, Scheduled }
enum class StepState { Done, Active, Pending, OnHold }
enum class MediaState { Uploading, Uploaded, Failed }
enum class MediaKind { Photo, Video, Audio }
enum class BadgeKind { Highlight, Status }
enum class ChatKind { Text, Image, Blocked }
enum class AvatarSize { Small, Medium, Large }

data class StatItem(@DrawableRes val icon: Int, val value: String, val label: String)
