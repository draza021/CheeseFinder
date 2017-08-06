/*
 * Copyright (c) 2016 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.raywenderlich.cheesefinder;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Cancellable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;

public class CheeseActivity extends BaseSearchActivity {
    private Disposable mDisposable;

    @Override
    protected void onStart() {
        super.onStart();

        Observable<String> buttonClickStream = createButtonClickObservable();
        Observable<String> textChangeStream = createTextChangeObservable();

        Observable<String> searchTextObservable = Observable.merge(textChangeStream, buttonClickStream);

        mDisposable = searchTextObservable
                // 1: Ensure that the next operator in chain will be run on the main thread.
                .observeOn(AndroidSchedulers.mainThread())
                // 2: Add the doOnNext operator so that showProgressBar() will be called every time a new item is emitted.
                .doOnNext(new Consumer<String>() {
                    @Override
                    public void accept(String s) {
                        showProgressBar();
                    }
                })
                .observeOn(Schedulers.io())
                .map(new Function<String, List<String>>() {
                    @Override
                    public List<String> apply(String query) {
                        return mCheeseSearchEngine.search(query);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<String>>() {
                    @Override
                    public void accept(List<String> result) {
                        // 3: Don’t forget to call hideProgressBar() when you are just about to display a result.
                        hideProgressBar();
                        showResult(result);
                    }
                });
    }

    // 1: You declare a method that returns an observable that will emit strings.
    private Observable<String> createButtonClickObservable() {

        // 2: You create an observable with Observable.create(), and supply it with a new ObservableOnSubscribe.
        return Observable.create(new ObservableOnSubscribe<String>() {

            // 3: You define your ObservableOnSubscribe by overriding subscribe().
            @Override
            public void subscribe(final ObservableEmitter<String> emitter) throws Exception {
                // 4: Set up an OnClickListener on mSearchButton.
                mSearchButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // 5: When the click event happens, call onNext on the emitter and pass it the current text value of mQueryEditText.
                        emitter.onNext(mQueryEditText.getText().toString());
                    }
                });

                // 6: Keeping references can cause memory leaks in Java. It’s a useful habit to remove listeners as soon as they are no longer needed.
                // But what do you call when you are creating your own Observable? For that very reason, ObservableEmitter has setCancellable(). Override cancel(),
                // and your implementation will be called when the Observable is disposed, such as when the Observable is completed or all Observers have unsubscribed from it.
                emitter.setCancellable(new Cancellable() {
                    @Override
                    public void cancel() throws Exception {
                        // 7: For OnClickListener, the code that removes the listener is setOnClickListener(null).
                        mSearchButton.setOnClickListener(null);
                    }
                });
            }
        });
    }

    //1: Declare a method that will return an observable for text changes.
    private Observable<String> createTextChangeObservable() {
        //2: Create textChangeObservable with create(), which takes an ObservableOnSubscribe.
        Observable<String> textChangeObservable = Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(final ObservableEmitter<String> emitter) throws Exception {
                //3: When an observer makes a subscription, the first thing to do is to create a TextWatcher.
                final TextWatcher watcher = new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void afterTextChanged(Editable s) {}

                    //4: You aren’t interested in beforeTextChanged() and afterTextChanged().
                    // When the user types and onTextChanged() triggers, you pass the new text value to an observer.
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        emitter.onNext(s.toString());
                    }
                };

                //5: Add the watcher to your TextView by calling addTextChangedListener().
                mQueryEditText.addTextChangedListener(watcher);

                //6: Don’t forget to remove your watcher. To do this, call emitter.setCancellable() and overwrite cancel() to call removeTextChangedListener()
                emitter.setCancellable(new Cancellable() {
                    @Override
                    public void cancel() throws Exception {
                        mQueryEditText.removeTextChangedListener(watcher);
                    }
                });
            }
        });

        // 7: Finally, return the created observable.
        return textChangeObservable
                .filter(new Predicate<String>() {
                    @Override
                    public boolean test(String query) throws Exception {
                        return query.length() >= 2;
                    }
                }).debounce(1000, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!mDisposable.isDisposed()) {
            mDisposable.dispose();
        }
    }
}
