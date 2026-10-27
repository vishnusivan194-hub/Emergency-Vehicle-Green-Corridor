# Emergency Vehicle Green Corridor

## Simulation notice

This is a fictional academic simulation. Road layouts, congestion, capacities, travel times, vehicle movement, reservations, signal events, and arrival predictions are sample or user-edited values. The project does not use live traffic feeds, GPS, real emergency vehicles, or traffic-light hardware, and it cannot control real signals.

## Start the dashboard

On Windows, double-click `run_project.bat`. It compiles the Java files in this folder and opens the dashboard. The launcher does not delete existing files.

Requirements: JDK 17 or newer and a desktop environment.

To launch from a terminal opened in this folder:

```powershell
javac *.java
java DashboardMain
```

There is no console menu to operate. The dashboard contains the route map and four tabs: Route, Conditions, Road Review, and Find.

## Dashboard guide

### Route

- Choose an emergency vehicle, start, destination, and a sample traffic scenario.
- Vehicle type affects how a simulated signal plan balances emergency waiting against delaying other traffic. It does not change the road route or road-only time, and changing it refreshes the prediction.
- Use **Calculate Best Route** to apply the selected options while keeping current road edits and reservations.
- Use **Load Preset** to restore the sample city and apply the selected traffic scenario.
- Use **Swap Endpoints** to reverse the selected locations.
- Click a map location with the left mouse button to set the start; right-click or Ctrl-click to set the destination.
- Turn **Show Road Times** on or off to display estimated time on every road.
- The route panel lists each road leg's sample base time, traffic-adjusted estimate, condition, cumulative arrival estimate, and available alternatives.
- The summary cards show road-only time, predicted corridor arrival, estimated time difference, and planned signal changes.
- Use **Play Route Visualization** for a simulated replay. It is not vehicle tracking.
- Use **Export Route Report** or Ctrl+E to save the current route and scenario report. Press F5 to recalculate.

Road colors stay visible even on the selected route: green/teal means light traffic (below 35%), orange means moderate traffic (35–69%), and a bold red line with a warning marker means heavy traffic (70% or higher). A dashed gray line with a red X means closed. A thin yellow stripe marks the selected route without hiding its traffic color. Road labels show each segment's estimated time. When a lower-congestion open route is available, heavy roads are avoided; if every available route needs a heavy road, the quickest open option is shown with a warning.

### Conditions

Choose a road and edit its traffic density and open/closed state. Select **Use this density for all three recent readings** to model sustained congestion rather than one recent change. Apply the road update to recalculate the display. Base time and capacity remain sample values used in travel-time estimates and capacity analysis.

The decision threshold controls when the simulated signal-priority plan is considered. Change the percentage and choose **Apply Decision Threshold** to see the effect. This setting is separate from the map's fixed heavy-road warning level.

Signal timing values are fixed to the sample simulation defaults. The **Signal Reservations** section lets you add a labeled time window at a selected intersection; overlapping reservations are rejected. Reservations affect the simulated signal-plan calculation. **Show Road Status** lists the current simulated road values. **Restore Sample** resets the sample city, road conditions, timing defaults, and reservations.

### Road Review

The table shows each road, traffic percentage, availability, estimated time, and condition. Edit traffic or open status directly, then choose **Apply / Recalculate from Road Table**. For a quick traffic demonstration, select a road row and choose **Light 20%**, **Moderate 50%**, or **Heavy 85%**. The road color, warning marker, table, and route update immediately.

Other actions:

- **Refresh Review** summarizes open-road connection groups, capacity limits, possible movement, and the selected route.
- **City Times** compares estimated travel time between every pair of locations.
- **Check Road Range** shows total, average, and highest traffic across the selected numbered range.
- **Show Detailed Feature View** displays route-search progress, signal-plan options, normal-traffic release choices, and plain-language notes about how the reviews work.
- **Open-Road Connection Groups** shows which locations remain connected. Close a road in Conditions and run it again to demonstrate a disconnection.

### Find

- **Find by Name** accepts a location name, a name prefix, or its number.
- **Nearest Location** finds the closest sample location to entered map coordinates.
- **Nearest Hospital** reports the closest hospital by map position and the quickest reachable hospital by simulated road time.

## Suggested classroom demonstration

1. Start the dashboard and point out the simulation notice.
2. On Route, show the sample Airport-to-City-Hospital trip, road times, route alternatives, and simulated replay.
3. Open Road Review and edit a road's traffic percentage or availability; apply the changes and point out the updated map and route.
4. Open Conditions and adjust the road condition or decision threshold. Add a signal reservation and observe its effect on the simulated route prediction.
5. Use Road Review to show open-road connection groups, city travel times, road-range summary, and capacity review.
6. Use Find to demonstrate name, coordinate, and hospital lookup.

