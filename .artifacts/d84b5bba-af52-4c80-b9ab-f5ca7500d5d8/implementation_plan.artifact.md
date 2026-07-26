# Fix argument type mismatch in SeasonProgressAdapter and other files

The project fails to build because `Utils.formatMinutesToHoursAndMinutes` signature was changed to require a `Context` parameter, but several callers were not updated.

## Proposed Changes

### [app component]

#### [MODIFY] [SeasonProgressAdapter.kt](file:///C:/Users/maksa/Desktop/Projects/MovieTimeTracker/app/src/main/java/com/example/movietime/ui/details/SeasonProgressAdapter.kt)
- Pass `context` to `Utils.formatMinutesToHoursAndMinutes` in `bind` and `updateSeasonInfo` methods.

#### [MODIFY] [TvDetailsActivity.kt](file:///C:/Users/maksa/Desktop/Projects/MovieTimeTracker/app/src/main/java/com/example/movietime/ui/details/TvDetailsActivity.kt)
- Pass `this` as context to all calls of `Utils.formatMinutesToHoursAndMinutes`.

#### [MODIFY] [TvProgressBottomSheet.kt](file:///C:/Users/maksa/Desktop/Projects/MovieTimeTracker/app/src/main/java/com/example/movietime/ui/details/TvProgressBottomSheet.kt)
- Pass `requireContext()` to all calls of `Utils.formatMinutesToHoursAndMinutes`.

#### [MODIFY] [MainViewModel.kt](file:///C:/Users/maksa/Desktop/Projects/MovieTimeTracker/app/src/main/java/com/example/movietime/ui/main/MainViewModel.kt)
- Remove `totalTimeFormatted` property as it is unused and cannot easily access `Context`.

#### [MODIFY] [Utils.kt](file:///C:/Users/maksa/Desktop/Projects/MovieTimeTracker/app/src/main/java/com/example/movietime/util/Utils.kt)
- Make `context` parameter nullable in `formatMinutesToHoursAndMinutes` and provide a fallback formatting for cases where context is null (like unit tests or ViewModels if needed). This will prevent crashes and fix the tests.

#### [MODIFY] [UtilsTest.kt](file:///C:/Users/maksa/Desktop/Projects/MovieTimeTracker/app/src/test/java/com/example/movietime/util/UtilsTest.kt)
- Pass `null` as context to `formatMinutesToHoursAndMinutes` calls in tests.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:compileDebugKotlin` to ensure all compilation errors are resolved.
- Run `./gradlew :app:testDebugUnitTest` to verify `UtilsTest` passes.

### Manual Verification
- Deploy the app and verify that time is correctly formatted in:
    - TV Details screen (Total watch time)
    - Season Progress screen (Season runtime)
    - Episode Progress bottom sheet
