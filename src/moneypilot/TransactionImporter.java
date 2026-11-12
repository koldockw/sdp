package moneypilot;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
public interface TransactionImporter { List<Transaction> read(Path path) throws IOException; }
