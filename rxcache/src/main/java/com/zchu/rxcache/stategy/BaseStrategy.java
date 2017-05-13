package com.zchu.rxcache.stategy;

import com.zchu.rxcache.CacheTarget;
import com.zchu.rxcache.RxCache;
import com.zchu.rxcache.data.CacheResult;
import com.zchu.rxcache.data.ResultFrom;
import com.zchu.rxcache.utils.LogUtils;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * 作者: 赵成柱 on 2016/9/12 0012.
 */
abstract class BaseStrategy implements IStrategy {

    <T> Observable<CacheResult<T>> loadCache(final RxCache rxCache, final String key) {
        return rxCache
                .<T>load(key)
                .onErrorReturn(new Function<Throwable, T>() {// rxjava 2.0 is not allowed null so if load(key) return null just return Observable.empty()
                    @Override
                    public T apply(@NonNull Throwable throwable) throws Exception {
                        return (T)Observable.empty();
                    }
                })
                .map(new Function<T, CacheResult<T>>() {
                    @Override
                    public CacheResult<T> apply(@NonNull T o) throws Exception {
                        LogUtils.debug("loadCache result=" + o);
                        return new CacheResult<>(ResultFrom.Cache, key,  o);
                    }
                });
    }

     <T> Observable<CacheResult<T>> loadRemote(final RxCache rxCache, final String key, Observable<T> source, final CacheTarget target) {
        return source
                .map(new Function<T, CacheResult<T>>() {
                    @Override
                    public CacheResult<T> apply(@NonNull T t) throws Exception {
                        LogUtils.debug("loadRemote result=" + t);
                        rxCache.save(key, t,target).subscribeOn(Schedulers.io())
                                .subscribe(new Consumer<Boolean>() {
                                    @Override
                                    public void accept(@NonNull Boolean status) throws Exception {
                                        LogUtils.debug("save status => " + status);
                                    }
                                });
                        return new CacheResult<>(ResultFrom.Remote, key, t);
                    }
                });
    }

    @Override
    public abstract <T> Observable<CacheResult<T>> execute(RxCache rxCache, String key, Observable<T> source);
}
