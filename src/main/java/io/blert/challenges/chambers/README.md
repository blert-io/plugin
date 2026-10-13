# Chambers of Xeric Room Time Tracking

This directory contains the implementation for the new Chambers of Xeric feature, which aims to track individual room times and feed them into the Blert data tracker for event handling and analytics.

## Feature Overview
- **Purpose:** Track the duration of each room in a Chambers of Xeric raid and record this data for further analysis and event handling.
- **Integration:** Room time data is collected and passed to the Blert data tracker, which is responsible for dispatching events and updating challenge state.

## Implementation Notes
- **Timer Logic:** The working logic for tracking room times currently resides in `CoxTimersPlugin.java`. This file is not directly related to the feature and serves only as a reference for timer logic. The relevant logic should be transplanted into the new data tracker class for Chambers of Xeric.
- **Data Tracker Structure:** The new data tracker should follow the general structure and design patterns used in the following files:
  - `DelveDataTracker.java` (Mokhaiotl)
  - `WaveDataTracker.java` (Inferno)
  - `RoomDataTracker.java` (Theatre of Blood)

  These files provide examples of how to:
  - Track room or wave progress
  - Handle event dispatching
  - Integrate with the Blert event handler
  - Manage state transitions and timing

## Development Guidelines
- **Class Design:** Implement the data tracker as an abstract or concrete class, following the conventions in the referenced examples.
- **Event Handling:** Ensure that room time events are dispatched to the event handler in a consistent manner.
- **State Management:** Properly manage room end, similar to the structure in the example trackers.
- **Documentation:** Document the code and logic clearly, referencing the timer logic source and the example trackers for maintainability.

## Next Steps
1. Review the timer logic in `CoxTimersPlugin.java` and extract the relevant portions for room time tracking.
2. Implement the new data tracker class in this directory, following the structure of the example files.
3. Integrate the tracker with the Blert event handler to ensure room times are recorded and dispatched correctly.

---

**References:**
- `CoxTimersPlugin.java` (timer logic reference)
- `DelveDataTracker.java`, `WaveDataTracker.java`, `RoomDataTracker.java` (example data trackers)
