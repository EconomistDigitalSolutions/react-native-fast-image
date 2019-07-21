import React, { Component } from 'react';
import PropTypes from 'prop-types';
import {
    View,
    Image,
    requireNativeComponent,
    ViewPropTypes,
    StyleSheet,
    NativeModules,
    Platform
} from 'react-native';
import preloaderManager from './preloaderManager';

class FastImage extends Component {
    setNativeProps(nativeProps) {
        this._root.setNativeProps(nativeProps);
    }

    captureRef = e => (this._root = e);

    render() {
        const {
            source,
            placeholder,
            onLoadStart,
            onProgress,
            onLoad,
            onError,
            onLoadEnd,
            style,
            children,
            fallback,
            ...props
        } = this.props;

        const resolvedSource =
            Platform.OS === 'android'
                ? {
                      source: Image.resolveAssetSource(source),
                      placeholder: Image.resolveAssetSource(placeholder)
                  }
                : Image.resolveAssetSource(source);
        const resolvedPlaceholder = Image.resolveAssetSource(placeholder);

        if (fallback) {
            return (
                <View style={[styles.imageContainer, style]} ref={this.captureRef}>
                    <FastImageView
                        {...props}
                        style={StyleSheet.absoluteFill}
                        source={resolvedSource}
                        placeholder={resolvedPlaceholder}
                        onLoadStart={onLoadStart}
                        onProgress={onProgress}
                        onLoad={onLoad}
                        onError={onError}
                        onLoadEnd={onLoadEnd}
                    />
                    {children}
                </View>
            );
        }

        return (
            <View style={[styles.imageContainer, style]} ref={this.captureRef}>
                <FastImageView
                    {...props}
                    style={StyleSheet.absoluteFill}
                    source={resolvedSource}
                    placeholder={resolvedPlaceholder}
                    onFastImageLoadStart={onLoadStart}
                    onFastImageProgress={onProgress}
                    onFastImageLoad={onLoad}
                    onFastImageError={onError}
                    onFastImageLoadEnd={onLoadEnd}
                />
                {children}
            </View>
        );
    }
}

const styles = StyleSheet.create({
    imageContainer: {
        overflow: 'hidden'
    }
});

FastImage.resizeMode = {
    contain: 'contain',
    cover: 'cover',
    stretch: 'stretch',
    center: 'center'
};

FastImage.priority = {
    // lower than usual.
    low: 'low',
    // normal, the default.
    normal: 'normal',
    // higher than usual.
    high: 'high'
};

FastImage.cacheControl = {
    // Ignore headers, use uri as cache key, fetch only if not in cache.
    immutable: 'immutable',
    // Respect http headers, no aggressive caching.
    web: 'web',
    // Only load from cache.
    cacheOnly: 'cacheOnly'
};

FastImage.preload = (
    sources,
    cacheControl = FastImage.cacheControl.immutable,
    onProgress,
    onComplete,
    preloadConfig
) => preloaderManager.preload(sources, cacheControl, onProgress, onComplete, preloadConfig);

FastImage.cancelPreload = preloaderId => {
    preloaderManager.cancelPreload(preloaderId);
};

FastImage.addReadOnlyCachePath = path => NativeModules.FastImageView.addReadOnlyCachePath(path);

FastImage.remove = namespace => preloaderManager.remove(namespace);

FastImage.defaultProps = {
    resizeMode: FastImage.resizeMode.cover
};

const FastImageSourcePropType = PropTypes.shape({
    uri: PropTypes.string,
    headers: PropTypes.objectOf(PropTypes.string),
    priority: PropTypes.oneOf(Object.keys(FastImage.priority)),
    cache: PropTypes.oneOf(Object.keys(FastImage.cacheControl)),
    placeholder: PropTypes.string
});

FastImage.propTypes = {
    ...ViewPropTypes,
    source: PropTypes.oneOfType([FastImageSourcePropType, PropTypes.number]),
    onLoadStart: PropTypes.func,
    onProgress: PropTypes.func,
    onLoad: PropTypes.func,
    onError: PropTypes.func,
    onLoadEnd: PropTypes.func,
    fallback: PropTypes.bool
};

const FastImageView = requireNativeComponent('FastImageView', FastImage, {
    nativeOnly: {
        onFastImageLoadStart: true,
        onFastImageProgress: true,
        onFastImageLoad: true,
        onFastImageError: true,
        onFastImageLoadEnd: true
    }
});

export default FastImage;
