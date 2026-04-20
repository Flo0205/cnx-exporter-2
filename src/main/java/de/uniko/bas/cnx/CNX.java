package de.uniko.bas.cnx;

import de.uniko.bas.cnx.objects.Community;
import de.uniko.bas.cnx.services.CommunityService;
import de.uniko.bas.cnx.services.InitService;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(
        name = "cnx",
        mixinStandardHelpOptions = true,
        version = "CNX-Exporter 2.0.1",
        description = "Exports a community from CNX."
)
public class CNX implements Callable<Integer> {

    private static final Exporter exporter = new Exporter();
    private static final CommunityService communityService = new CommunityService();

    @Parameters(
            index = "0",
            paramLabel = "COMMUNITY_ID",
            description = "The community ID to export."
    )
    private String communityId;

    @Option(
            names = "--host",
            description = "CNX host. Falls back to env: CNX_HOST"
    )
    private String host;

    @Option(
            names = {"-u", "--user"},
            description = "CNX username. Falls back to env: CNX_USER"
    )
    private String username;

    @Option(
            names = {"-p", "--password"},
            interactive = true,
            arity = "0..1",
            description = "CNX password. Leave blank for interactive input. Falls back to env: CNX_PASS"
    )
    private String password;

    @Option(
            names = "--out-dir",
            description = "Export directory. Leave blank for default 'export'. Falls back to env: CNX_OUTDIR",
            defaultValue = "export"
    )
    private String outDir;

    @Option(
            names = {"-s", "--single"},
            description = "Export community as single JSON file"
    )
    private boolean singleOutput;

    @Override
    public Integer call() throws Exception {
        String resolvedHost = firstNonBlank(host, System.getenv("CNX_HOST"));
        String resolvedUser = firstNonBlank(username, System.getenv("CNX_USER"));
        String resolvedPass = firstNonBlank(password, System.getenv("CNX_PASS"));
        String resolvedOutDir = firstNonBlank(outDir, System.getenv("CNX_OUTDIR"));

        if (isBlank(resolvedHost) || isBlank(resolvedUser) || isBlank(resolvedPass)) {
            System.err.println("Missing required connection settings. Provide:");
            System.err.println("  --host, --user, --password");
            System.err.println("or set environment variables:");
            System.err.println("  CNX_HOST, CNX_USER, CNX_PASS");
            return 1;
        }

        Config.URLS.put("host", resolvedHost);
        Config.AUTH = new Config.Auth<>(resolvedUser, resolvedPass);

        if (!isBlank(resolvedOutDir)) {
            Config.EXPORTDIR = resolvedOutDir;
        }

        // Get Service URLs
        InitService.getServiceConfigs();

        // Load community
        Community com = communityService.getCommunity(communityId);

        // Export community
        if (singleOutput) {
            exporter.exportCommunity(com);
        }
        else {
            exporter.exportCommunitySplit(com);
        }

        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new CNX()).execute(args);
        System.exit(exitCode);
    }

    // Helper

    private static String firstNonBlank(String first, String fallback) {
        return !isBlank(first) ? first : fallback;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}