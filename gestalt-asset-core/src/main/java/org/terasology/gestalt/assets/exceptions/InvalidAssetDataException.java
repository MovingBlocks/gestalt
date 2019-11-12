/*
 * Copyright 2019 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terasology.gestalt.assets.exceptions;

/**
 * Thrown to indicate when asset data was not valid for loading or reloading an asset
 *
 * @author Immortius
 */
public class InvalidAssetDataException extends RuntimeException {
    public InvalidAssetDataException() {
    }

    public InvalidAssetDataException(String message) {
        super(message);
    }

    public InvalidAssetDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidAssetDataException(Throwable cause) {
        super(cause);
    }

    public InvalidAssetDataException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
