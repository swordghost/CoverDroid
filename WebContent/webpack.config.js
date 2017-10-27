let Webpack = require('webpack');
let path = require('path');
let srcPath = path.resolve(__dirname, 'src');
let nodeModulesPath = path.resolve(__dirname, 'node_modules');

let config = {
    context: __dirname,
    devtool: 'source-map',
    entry: path.resolve(srcPath, 'entry.js'),
    output: {
        path: path.resolve(__dirname, 'js'),
        filename: "bundle.js"
    },
    externals: {
        jquery: 'window.$'
    },
    module: {
        //加载器配置
        loaders: [
            {test: /\.js$/, loader: 'babel', exclude: [nodeModulesPath]},
            {test: /\.hbs$/, loader: 'handlebars'},
            {test: /\.css$/, loader: 'style!css'},
            {test: /\.scss$/, loader: 'style!css!sass?outputStyle=expanded'},
            {test: /\.(png|jpg)$/, loader: 'url-loader?limit=8192'}
        ]
    },
    resolve: {
        alias: {
            // third-parties
            'backbone': path.resolve(nodeModulesPath, 'backbone'),
            'marionette': 'backbone.marionette'
        },
        // 现在你require文件的时候可以直接使用require('file')，不用使用require('file.coffee')
        extensions: ['', '.js', '.json', '.coffee']
    }
};

module.exports = config;