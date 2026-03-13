config.devServer = config.devServer || {};
config.devServer.allowedHosts = "all";
config.devServer.historyApiFallback = true;
config.devServer.proxy = [
    {
        context: ["/api"],
        target: "http://localhost:8081",
    },
];
