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

import android.view.View;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Cancellable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class CheeseActivity extends BaseSearchActivity {

    @Override
    protected void onStart() {
        super.onStart();

        Observable<String> searchTextObservable = createButtonClickObservable();

        searchTextObservable
                // 1: First, specify that the next operator should be called on the I/O thread.
                .observeOn(Schedulers.io())
                // 2: For each search query, you return a list of results.
                .map(new Function<String, List<String>>() {
                    @Override
                    public List<String> apply(String query) {
                        return mCheeseSearchEngine.search(query);
                    }
                })
                // 3: Finally, specify that code down the chain should be executed on the main thread instead of on the I/O thread.
                // In Android, all code that works with Views should execute on the main thread.
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<String>>() {
                    @Override
                    public void accept(List<String> result) {
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

}
