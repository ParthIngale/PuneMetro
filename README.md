## Getting Started

Welcome to the VS Code Java world. Here is a guideline to help you get started to write Java code in Visual Studio Code.

## Folder Structure

The workspace contains two folders by default, where:

- `src`: the folder to maintain sources
- `lib`: the folder to maintain dependencies

Meanwhile, the compiled output files will be generated in the `bin` folder by default.

> If you want to customize the folder structure, open `.vscode/settings.json` and update the related settings there.

## Dependency Management

The `JAVA PROJECTS` view allows you to manage your dependencies. More details can be found [here](https://github.com/microsoft/vscode-java-dependency#manage-dependencies).


private void findRoute() {
        String source = sourceComboBox.getValue();
        String dest = destinationComboBox.getValue();

        if (source == null || dest == null || source.isEmpty() || dest.isEmpty()) {
            resultArea.setText("Please select both source and destination stations.");
            return;
        }

        int sourceIdx = getStationIndex(source);
        int destIdx = getStationIndex(dest);

        if (sourceIdx < 0 || destIdx < 0) {
            resultArea.setText("No route found.");
            return;
        }

        dijkstra(sourceIdx, destIdx);

        // Build route list from predecessor array
        List<String> route = new ArrayList<>();
        int current = destIdx;
        while (current != sourceIdx) {
            route.add(getStationName(current));
            if (predecessor[current] == 0 && current != sourceIdx) {
                resultArea.setText("No route found.");
                return;
            }
            current = predecessor[current];
        }
        route.add(getStationName(sourceIdx));
        Collections.reverse(route);

        // --- Travel Time Calculation ---
        int numberOfHops = route.size() - 1;
        int estimatedTime = numberOfHops * 2; // 2 minutes per hop
        int interchanges = 0;

        // Build color map for stations
        Map<String, String> stationColorMap = new HashMap<>();
        for (int i = 0; i <= 48; i++) {
            stationColorMap.put(getStationName(i), color[i]);
        }

        for (int i = 1; i < route.size() - 1; i++) {
            String prev = route.get(i - 1);
            String curr = route.get(i);
            String next = route.get(i + 1);

            if (curr.equalsIgnoreCase("Civil Court")) {
                String prevLine = stationColorMap.get(prev);
                String nextLine = stationColorMap.get(next);

                if (prevLine != null && nextLine != null && !prevLine.equals(nextLine)) {
                    interchanges++;
                    estimatedTime += 5; // add 5 min for interchange
                }
            }
        }

        // Display route
        StringBuilder routeStr = new StringBuilder("Route: ");
        for (int i = 0; i < route.size(); i++) {
            routeStr.append(route.get(i));
            if (i != route.size() - 1) {
                routeStr.append(" → ");
            }
        }

        routeStr.append("\n\nEstimated Time: ").append(estimatedTime).append(" minutes");
        if (interchanges > 0) {
            routeStr.append(" (Includes ").append(interchanges).append(" interchange")
                    .append(interchanges > 1 ? "s" : "").append(" at Civil Court)");
        }

        resultArea.setText(routeStr.toString());
    }
