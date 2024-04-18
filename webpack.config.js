const path = require('path');
const webpack = require('webpack');
const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin;
const {CleanWebpackPlugin} = require('clean-webpack-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const {CycloneDxWebpackPlugin} = require('@cyclonedx/webpack-plugin');

/** @type {import('@cyclonedx/webpack-plugin').CycloneDxWebpackPluginOptions} */
const cycloneDxWebpackPluginOptions = {
    specVersion: '1.4',
    rootComponentType: 'library',
    outputLocation: './bom',
    validateResults: false
};

module.exports = (env, argv) => {
    let config = {
        entry: {
            fancybox: [path.resolve(__dirname, 'src/javascript/fancybox.js')],
            session: [path.resolve(__dirname, 'src/javascript/session.js')],
            datatable: [path.resolve(__dirname, 'src/javascript/datatable.js')],
            definitions: [path.resolve(__dirname, 'src/javascript/definitions.js')]
        },
        output: {
            path: path.resolve(__dirname, 'src/main/resources/javascript/apps/'),
            filename: '[name].tools.bundle.js',
            chunkFilename: '[name].tools.[chunkhash:6].js'
        },
        resolve: {
            mainFields: ['module', 'main'],
            extensions: ['.mjs', '.js', '.jsx', '.json'],
            fallback: {
                "fs": false,
                "os": false,
                "path": false,
                "net": false,
                "tls": false,
            }
        },
        module: {
            rules: [
                {
                    test: /\.m?js$/,
                    type: 'javascript/auto'
                },
                {
                    test: /\.jsx?$/,
                    include: [path.join(__dirname, 'src')],
                    use: {
                        loader: 'babel-loader',
                        options: {
                            presets: [
                                ['@babel/preset-env', {
                                    modules: false,
                                    targets: {chrome: '60', edge: '44', firefox: '54', safari: '12'}
                                }],
                                '@babel/preset-react'
                            ],
                            plugins: [
                                '@babel/plugin-syntax-dynamic-import'
                            ]
                        }
                    }
                },
                {
                    test: /\.css$/i,
                    use: ["style-loader","css-loader"],
                }
            ]
        },
        plugins: [
            new CleanWebpackPlugin({verbose: false}),
            new CopyWebpackPlugin({patterns: [{from: './package.json', to: ''}]}),
            new CycloneDxWebpackPlugin(cycloneDxWebpackPluginOptions),
        ],
        mode: argv.mode ? argv.mode : 'development'
    };

    config.devtool = (argv.mode === 'production') ? 'source-map' : 'eval-source-map';

    if (argv.analyze) {
        config.devtool = 'source-map';
        config.plugins.push(new BundleAnalyzerPlugin());
    }

    return config;
};
