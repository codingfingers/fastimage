/* Copyright (c) 2012 Coding Fingers S. C. Marcin �picki i Daniel Dudek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codingfingers.fastimagelist.cache;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Set;

public class CacheHelper {

    public static String getFileNameFromUrl(String url) {
        // replace all special URI characters with a single + symbol
        return url.replaceAll("[.:/,%?&=]", "+").replaceAll("[+]+", "+");
    }

    public static void removeAllWithStringPrefix(AbstractCache<String, ?> cache, String urlPrefix) {
        Set<String> keys = cache.keySet();

        for (String key : keys) {
            if (key.startsWith(urlPrefix)) {
                cache.remove(key);
            }
        }

        if (cache.isDiskCacheEnabled()) {
            removeExpiredCache(cache, urlPrefix);
        }
    }

    private static void removeExpiredCache(final AbstractCache<String, ?> cache,
            final String urlPrefix) {
        final File cacheDir = new File(cache.getDiskCacheDirectory());

        if (!cacheDir.exists()) {
            return;
        }

        File[] list = cacheDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return dir.equals(cacheDir)
                        && filename.startsWith(cache.getFileNameForKey(urlPrefix));
            }
        });

        if (list == null || list.length == 0) {
            return;
        }

        for (File file : list) {
            file.delete();
        }
    }

}
