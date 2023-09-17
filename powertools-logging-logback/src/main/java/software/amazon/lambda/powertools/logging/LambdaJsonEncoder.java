/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package software.amazon.lambda.powertools.logging;

import static java.nio.charset.StandardCharsets.UTF_8;

import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.encoder.EncoderBase;
import software.amazon.lambda.powertools.logging.internal.LambdaJsonSerializer;

/**
 * Custom encoder for logback that encodes logs in JSON format.
 * It does not use a JSON library but a custom serializer ({@link LambdaJsonSerializer}) to reduce the weight of the library.
 */
public class LambdaJsonEncoder extends EncoderBase<ILoggingEvent> {

    private final ThrowableProxyConverter throwableProxyConverter = new ThrowableProxyConverter();
    protected ThrowableHandlingConverter throwableConverter = null;
    protected String timestampFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZz";
    protected String timestampFormatTimezoneId = null;
    private boolean includeThreadInfo = false;

    @Override
    public byte[] headerBytes() {
        return null;
    }

    @Override
    public void start() {
        super.start();
        throwableProxyConverter.start();
        if (throwableConverter != null) {
            throwableConverter.start();
        }
    }

    @Override
    public byte[] encode(ILoggingEvent event) {
        StringBuilder builder = new StringBuilder(256);
        LambdaJsonSerializer.serializeObjectStart(builder);
        LambdaJsonSerializer.serializeLogLevel(builder, event.getLevel());
        LambdaJsonSerializer.serializeFormattedMessage(builder, event.getFormattedMessage());
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            if (throwableConverter != null) {
                LambdaJsonSerializer.serializeException(builder, throwableProxy.getClassName(),
                        throwableProxy.getMessage(), throwableConverter.convert(event));
            } else if (throwableProxy instanceof ThrowableProxy) {
                LambdaJsonSerializer.serializeException(builder, ((ThrowableProxy) throwableProxy).getThrowable());
            } else {
                LambdaJsonSerializer.serializeException(builder, throwableProxy.getClassName(),
                        throwableProxy.getMessage(), throwableProxyConverter.convert(event));
            }
        }
        LambdaJsonSerializer.serializePowertools(builder, event.getMDCPropertyMap());
        if (includeThreadInfo) {
            LambdaJsonSerializer.serializeThreadName(builder, event.getThreadName());
            LambdaJsonSerializer.serializeThreadId(builder, String.valueOf(Thread.currentThread().getId()));
            LambdaJsonSerializer.serializeThreadPriority(builder, String.valueOf(Thread.currentThread().getPriority()));
        }
        LambdaJsonSerializer.serializeTimestamp(builder, event.getTimeStamp(), timestampFormat,
                timestampFormatTimezoneId);
        LambdaJsonSerializer.serializeObjectEnd(builder);
        return builder.toString().getBytes(UTF_8);
    }

    @Override
    public byte[] footerBytes() {
        return null;
    }

    public void setTimestampFormat(String timestampFormat) {
        this.timestampFormat = timestampFormat;
    }

    public void setTimestampFormatTimezoneId(String timestampFormatTimezoneId) {
        this.timestampFormatTimezoneId = timestampFormatTimezoneId;
    }

    public void setThrowableConverter(ThrowableHandlingConverter throwableConverter) {
        this.throwableConverter = throwableConverter;
    }

    public void setIncludeThreadInfo(boolean includeThreadInfo) {
        this.includeThreadInfo = includeThreadInfo;
    }
}