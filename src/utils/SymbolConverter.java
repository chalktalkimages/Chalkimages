package utils;

public final class SymbolConverter {

  private static String[] SYMBOL_SUFFIX = {
    ".TSXV", ".TSX", ".NYS", ".NAS", "ARC", ".PF", ".UN", ".DB", ".WT", ".PR", ".RT"
  };
  private static String[] REUTERS_SUFFIX = {
    ".V", ".TO", ".N", ".O", ".P", "_pf", "_u", "db", "_t", "_p", "_r"
  };

  public static String RIC2Symbol(String ric) {
    if (ric == null || ric.length() == 0) return "";
    String symbol = ric;
    for (int i = 0; i < REUTERS_SUFFIX.length; i++) {
      if (REUTERS_SUFFIX[i].equals("_pf") && ric.contains("_pf.")) continue;
      symbol = symbol.replace(REUTERS_SUFFIX[i], SYMBOL_SUFFIX[i]);
    }

    // series and class
    symbol = symbol.replaceAll("([a-z].)", "\\.$1").toUpperCase();
    return symbol;
  }

  public static String getPrefix(String symbol) {
    int i = symbol.lastIndexOf(".");
    return i == -1 ? symbol : symbol.substring(0, i);
  }

  // Converts symbols to HTML special characters
  public static String htmlSymbolReplace(String message) {

    return "";
  }
}
