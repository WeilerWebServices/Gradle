/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
true ?: 'a'

true
?: 'a'

true
?:
'a'

true ? 'a' : 'b'

true ?
        'a'
        :
        'b'

true ?
        'a'
        :
        true ?: 'b'

true ?
        'a'
        :
        true ? 'b' : 'c'

true ?: true ?: 'a'

1 == 2 ?: 3
1 == 2 ? 3 : 4

1 == 2 || 1 != 3 && !(1 == 6)
    ? 2 > 3 && 3 >= 1
        ? 4 < 5 && 2 <= 9 ?: 6 ^ 8 | 9 & 10
        : 8 * 2 / (3 % 4 + 6) - 2
    : 9


bar = 0 ? "moo"         \
              : "cow"
