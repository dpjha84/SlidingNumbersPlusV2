using System.Text.Json;
using System.Text.Json.Serialization;

namespace SlidingNumbersPlus.Models
{
    public class GameState
    {
        private const string Key = "snp_v1";
        [JsonInclude] public int Level     { get; set; } = 1;
        [JsonInclude] public int MaxLevel  { get; set; } = 1;
        [JsonInclude] public int StarCount { get; set; } = 0;
        [JsonInclude] public int MovesLeft { get; set; } = -1;
        [JsonInclude] public bool SoundOn  { get; set; } = true;
        [JsonInclude] public bool MusicOn  { get; set; } = false;
        [JsonInclude] public List<string> Sequence { get; set; } = new();

        [JsonIgnore] public int GridSize  => (Level - 1) / 5 + 3;
        [JsonIgnore] public int Increment => Level % 5 != 0 ? Level % 5 : 5;
        [JsonIgnore] public int MaxMoves  => (int)(Math.Pow(2, GridSize) * (Increment + 4));

        public static GameState Load()
        {
            try
            {
                var json = Preferences.Default.Get(Key, string.Empty);
                if (!string.IsNullOrEmpty(json))
                {
                    var s = JsonSerializer.Deserialize<GameState>(json);
                    if (s != null) return s;
                }
            }
            catch { }
            return new GameState();
        }

        public void Save()
        {
            try { Preferences.Default.Set(Key, JsonSerializer.Serialize(this)); }
            catch { }
        }

        public static List<string> DefaultSequence(int size)
        {
            var list = new List<string>();
            for (int i = 1; i < size * size; i++) list.Add(i.ToString());
            list.Add(string.Empty);
            return list;
        }

        public static string CityName(int gridSize) => gridSize switch
        { 3 => "London", 4 => "Paris", _ => "New York" };

        public static string CityImage(int gridSize) => gridSize switch
        { 3 => "london.png", 4 => "paris.png", _ => "newyork.png" };
    }
}
