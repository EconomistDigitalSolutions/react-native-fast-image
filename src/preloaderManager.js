import { NativeEventEmitter, NativeModules } from 'react-native'
const nativeManager = NativeModules.FastImagePreloaderManager
const nativeEmitter = new NativeEventEmitter(nativeManager)

class PreloaderManager {
    _instances = new Map()
    _subProgress = null
    _subComplete = null

    preload = (sources, cacheControl, onProgress, onComplete, preloadConfig) => {
        return nativeManager.createPreloaderWithConfig(preloadConfig).then(id => {
            if (this._instances.size === 0) {
                this._subProgress = nativeEmitter.addListener(
                    'fffastimage-progress',
                    this.onProgress,
                )
                this._subComplete = nativeEmitter.addListener(
                    'fffastimage-complete',
                    this.onComplete,
                )
            }
            this._instances.set(id, { onProgress, onComplete })
            nativeManager.preload(id, sources, cacheControl)
            return id;
        })
    }

    remove = source => nativeManager.remove(source)

    cancelPreload = id => {
        const instance = this._instances.get(id)
        if (instance) {
            nativeManager.cancelPreload(id)
        }
    }

    onProgress = ({ id, finished, total }) => {
        const instance = this._instances.get(id)
        if (instance) {
            const { onProgress } = instance
            if (onProgress) {
                onProgress(finished, total)
            }
        }
    }

    onComplete = ({ id, finished, skipped }) => {
        const instance = this._instances.get(id)
        if (instance) {
            const { onComplete } = instance
            if (onComplete) {
                onComplete(finished, skipped)
            }
        }
        this._instances.delete(id)
        if (this._instances.size === 0) {
            this._subProgress.remove()
            this._subComplete.remove()
        }
    }
}

const preloaderManager = new PreloaderManager()
export default preloaderManager
