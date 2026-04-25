using CommunityToolkit.Mvvm.ComponentModel;
using SlidingNumbersPlus.Models;
using SlidingNumbersPlus.Services;

namespace SlidingNumbersPlus.ViewModels
{
    public partial class GameViewModel : ObservableObject
    {
        private readonly PuzzleEngine _engine = new();
        private System.Timers.Timer? _timer;

        [ObservableProperty] private int _movesLeft;
        [ObservableProperty] private int _starCount;
        [ObservableProperty] private int _level;
        [ObservableProperty] private int _starsEarned;
        [ObservableProperty] private string _cityName = string.Empty;
        [ObservableProperty] private string _bgImage = string.Empty;
        [ObservableProperty] private string _movesColor = "#FFFFFF";
        [ObservableProperty] private bool _isLoading = true;

        public int GridSize => _engine.Size;

        public event Action<List<Tile>>? TilesUpdated;
        public event Action? GameReady;
        public event Action<int>? WinTriggered;
        public event Action? FailTriggered;

        public void StartGame(GameState state, bool newGame)
        {
            Level = state.Level;
            StarCount = state.StarCount;
            CityName = GameState.CityName(state.GridSize);
            BgImage = GameState.CityImage(state.GridSize);
            IsLoading = true;

            var seq = (!newGame && state.Sequence.Count > 0)
                ? state.Sequence
                : GameState.DefaultSequence(state.GridSize);

            _engine.Init(state.GridSize, seq);
            MovesLeft = (state.MovesLeft > 0 && !newGame) ? state.MovesLeft : state.MaxMoves;
            MovesColor = "#FFFFFF";

            if (newGame) ShuffleThenStart(state);
            else Ready();
            SentryHelper.Send(() => SentrySdk.CaptureMessage($"Starting level: {state.Level}, grid size: {state.GridSize}"));
        }

        private async void ShuffleThenStart(GameState state)
        {
            int moves = state.GridSize * (int)Math.Pow(2, state.Increment) * 20;
            System.Diagnostics.Debug.WriteLine($"Shuffling {moves} moves. GridSize={state.GridSize} Increment={state.Increment}");
            await Task.Run(() => _engine.Shuffle(moves));
            //await Task.Run(() => _engine.Shuffle(1));
            MovesLeft = state.MaxMoves;
            IsLoading = false;
            Ready();
        }

        private void Ready()
        {
            IsLoading = false;
            TilesUpdated?.Invoke(_engine.AllTiles());
            StartTimer();
            GameReady?.Invoke();
        }

        public void Tap(int row, int col)
        {
            if (IsLoading || MovesLeft <= 0 || _engine.IsSolved()) return;
            var changed = _engine.TryMove(row, col);
            if (changed.Count == 0) return;

            TilesUpdated?.Invoke(changed);
            MovesLeft--;
            MovesColor = MovesLeft <= 5 ? "#FF4444"
                       : MovesLeft <= 10 ? "#FFAA00"
                       : "#FFFFFF";
            SaveProgress();
            CheckEnd();
        }

        private void CheckEnd()
        {
            if (_engine.IsSolved())
            {
                StopTimer();
                StarsEarned = CalcStars();
                var s = GameState.Load();
                s.StarCount += StarsEarned;
                s.Level = s.Level + 1;
                s.MaxLevel = Math.Max(s.Level, s.MaxLevel);
                s.MovesLeft = -1;
                s.Sequence = new List<string>();
                s.Save();
                StarCount = s.StarCount;
                WinTriggered?.Invoke(StarsEarned);
                SentryHelper.Send(() => SentrySdk.CaptureMessage($"Won level: {s.Level}, moves left: {s.MovesLeft}"));
                return;
            }
            if (MovesLeft <= 0)
            {
                StopTimer();
                var s = GameState.Load();
                s.MovesLeft = -1; s.Sequence = new List<string>(); s.Save();
                SentryHelper.Send(() => SentrySdk.CaptureMessage($"Failed level: {s.Level}"));
                FailTriggered?.Invoke();
            }
        }

        public bool CanBuyMoves() => GameState.Load().StarCount >= 5;

        public void BuyMoves()
        {
            var s = GameState.Load();
            if (s.StarCount < 5) return;
            s.StarCount -= 5; s.Save();
            StarCount = s.StarCount;
            MovesLeft += 20;
            MovesColor = "#FFFFFF";
            StartTimer();
        }

        private int CalcStars()
        {
            var s = GameState.Load();
            int max = s.MaxMoves;
            if (MovesLeft <= 0) return 0;
            if (MovesLeft <= max / 3) return 1;
            if (MovesLeft <= 2 * max / 3) return 2;
            return 3;
        }

        private void StartTimer()
        {
            _timer?.Dispose();
            _timer = new System.Timers.Timer(2000);
            _timer.Elapsed += (_, _) => SaveProgress();
            _timer.Start();
        }

        public void StopTimer()
        {
            _timer?.Stop();
            _timer?.Dispose();
            _timer = null;
        }

        private void SaveProgress()
        {
            var s = GameState.Load();
            s.MovesLeft = MovesLeft;
            s.Sequence = _engine.Sequence();
            s.Save();
        }

        public List<Tile> AllTiles() => _engine.AllTiles();
    }
}