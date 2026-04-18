# Sliding Numbers +

A .NET 10 MAUI sliding puzzle game.

## Setup

1. Open `SlidingNumbersPlus.csproj` in Visual Studio 2022+
2. Restore NuGet packages (CommunityToolkit.Maui, CommunityToolkit.Mvvm)
3. Copy `OpenSans-Regular.ttf` and `OpenSans-Semibold.ttf` into `Resources/Fonts/`
   - These ship with every .NET MAUI template — grab from any new MAUI project
4. Optionally replace the placeholder city images in `Resources/Images/`:
   - `city_london.png`  — any London cityscape photo
   - `city_paris.png`   — any Paris cityscape photo
   - `city_newyork.png` — any New York cityscape photo
   - The game works without real photos (solid color fallbacks are provided)
5. Build and run on Android

## Architecture

- `Models/GameState.cs`       — Persisted game state via MAUI Preferences API
- `Services/PuzzleEngine.cs`  — Pure puzzle logic (no UI). Always-solvable shuffle.
- `ViewModels/GameViewModel.cs` — MVVM with CommunityToolkit.Mvvm
- `Views/`                    — HomePage, GamePage, LevelsPage, SettingsPage

## Features

- 3×3 / 4×4 / 5×5 grids (levels 1-5, 6-10, 11+)
- Correct tile highlighting (purple = correct position)
- Move counter with colour warnings (orange at 10, red at 5)
- 1-3 star rating per level based on efficiency
- Buy 20 extra moves for 5 stars when you run out
- Full progress persistence (level, stars, in-progress board)
- Settings: sound toggle, music toggle, reset progress
- Level picker showing locked/unlocked/current levels per city
