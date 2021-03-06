package ga.overfullstack.pokemon.before;

import java.util.Map;
import ga.overfullstack.pokemon.DBUtil;
import ga.overfullstack.pokemon.HttpUtil;
import kotlin.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static ga.overfullstack.pokemon.before.App.POKEMON_LIMIT_TO_FETCH;
import static ga.overfullstack.pokemon.before.App.POKEMON_OFFSET_TO_FETCH;

public class PokemonCollector {
  private static final Logger logger = LoggerFactory.getLogger(PokemonCollector.class);

  private PokemonCollector() {
  }

  public static void main(String[] args) {
    play(POKEMON_OFFSET_TO_FETCH, POKEMON_LIMIT_TO_FETCH);
  }

  /**
   * POOP (Procedure Oriented Programming)
   */
  public static Map<String, String> play(int pokemonOffsetToFetch, int pokemonLimitToFetch) {
    validate(pokemonOffsetToFetch, pokemonLimitToFetch);

    // Fetch all Pokémon
    final var fetchedPokemonNames = HttpUtil.fetchAllPokemon(pokemonOffsetToFetch, pokemonLimitToFetch);
    logger.info("Pokémon fetched: {}", fetchedPokemonNames);

    // Find DB match for fetched Pokémon.
    final var existingPokemonNameToPower = DBUtil.queryPokemonPowers(fetchedPokemonNames);
    logger.info("{} Matching Pokémon with Powers in DB: {}", existingPokemonNameToPower.size(), existingPokemonNameToPower);

    // Fetch powers for missing Pokémon (fetchedPokemonNames - existingPokemonNames).
    final var missingPokemonNames = fetchedPokemonNames.stream()
        .filter(key -> !existingPokemonNameToPower.containsKey(key)).toList();
    logger.info("Fetch for {} missing Pokémon: {}", missingPokemonNames.size(), missingPokemonNames);
    final var newPokemonToInsert = missingPokemonNames.stream()
        .map(pokemonName -> new Pair<>(pokemonName, HttpUtil.fetchPokemonPower(pokemonName))).toList();

    // Insert new fetched Pokémon into the DB.
    DBUtil.batchInsertPokemonPowers(newPokemonToInsert);

    // Fetch all collected Pokémon in DB.
    final var allPokemonWithPowers = DBUtil.queryAllPokemonPowers();
    logger.info("{} Pokémon with Powers in DB: {}", allPokemonWithPowers.size(), allPokemonWithPowers);
    return allPokemonWithPowers;
  }

  static void validate(int pokemonOffsetToFetch, int pokemonLimitToFetch) {
    if (pokemonOffsetToFetch < 0 || pokemonLimitToFetch < 0 || pokemonLimitToFetch > 10) {
      throw new IllegalArgumentException("Invalid offset or limit : offset=" + pokemonOffsetToFetch + ", limit=" + pokemonLimitToFetch);
    }
  }

}
