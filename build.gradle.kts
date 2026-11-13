// ArtisanGuilds, the guilds plugin on Artisan (successor of MSGuilds).
// The MSGuilds sources under src/ are not built any more: they are the reference while the
// services move into plugin/, and are deleted once the move is over.

allprojects {
    group = "com.github.ibanetchep"
    // A release sets it from its tag v<version> (-PpluginVersion, .github/workflows/release.yml).
    version = (findProperty("pluginVersion") as String?) ?: "2.0.0-SNAPSHOT"
}
