(function () {
	var module = angular.module("org.gradle.profile.listener.processor", []);

    // How many builds to process at one time.
    // If running with very fast network connection to the server, 
    // this number can be increased for better throughput.
    const MAX_CONCURRENT_BUILDS_TO_PROCESS = 6;    

    const event = function (events, id) {
        var event = events[id];
        if (event) {
            return event;
        } else {
            event = {};
            events[id] = event;
            return event;
        }
    };

    class PackingAndUnpackingHandler {
        constructor(build) {
            this.ignoreTags = !!build.ignoreTags;
            this.buildId = build.buildId;
            this.gradleEnterpriseServerUrl = build.gradleEnterpriseServerUrl;
            this.packEvents = {};
            this.unpackEvents = {};
        }

        onUserTag(eventPayload) {
            const tag = eventPayload.data.tag;
            if (tag === "gradle-profiler") {
                this.tagged = true;
            }
        }

        onUserNamedValue(eventPayload) {
            const key = eventPayload.data.key;
            const value = eventPayload.data.value;
            switch (key) {
                case "org.gradle.profiler.scenario":
                    this.scenario = value;
                    break;
                case "org.gradle.profiler.phase":
                    this.phase = value;
                    break;
                case "org.gradle.profiler.step":
                    this.step = value;
                    break;
                case "org.gradle.profiler.number":
                    this.number = parseInt(value);
                    break;
                default:
                    break;
            }
        }

        onBuildStarted(eventPayload) {
            this.buildStarted = eventPayload.timestamp;
        }

        onBuildRequestedTasks(eventPayload) {
            this.tasks = eventPayload.data.requested.join(", ");
        }

        onTaskGraphCalculationFinished(eventPayload) {
            this.taskExecutionStarted = eventPayload.timestamp;
        }

        onBuildCachePackStarted(eventPayload) {
            event(this.packEvents, eventPayload.data.id).start = eventPayload.timestamp;
        }

        onBuildCachePackFinished(eventPayload) {
            event(this.packEvents, eventPayload.data.id).end = eventPayload.timestamp;
        }

        onBuildCacheUnpackStarted(eventPayload) {
            event(this.unpackEvents, eventPayload.data.id).start = eventPayload.timestamp;
        }

        onBuildCacheUnpackFinished(eventPayload) {
            event(this.unpackEvents, eventPayload.data.id).end = eventPayload.timestamp;
        }

        onBuildFinished(eventPayload) {
            this.buildFinished = eventPayload.timestamp;
        }

        onBasicMemoryStats(eventPayload) {
            this.gcTime = eventPayload.data.gcTime;
        }

        complete(completionListener) {
            if (!this.ignoreTags && !this.tagged) {
                return;
            }

            const sum = function (events) {
                return Object.values(events).reduce((sum, event) => sum + event.end - event.start, 0);
            }

            var scenario = this.scenario;
            var phase = this.phase;
            var number = this.number;
            var step = this.step;
            var tasks = this.tasks;
            var buildScan = this.gradleEnterpriseServerUrl + "/s/" + this.buildId;
            var executionTime = this.buildFinished - this.buildStarted;
            var taskExecutionTime = this.buildFinished - this.taskExecutionStarted;
            var packTime = sum(this.packEvents);
            var unpackTime = sum(this.unpackEvents)
            var gcTime = this.gcTime;
            completionListener({
                scenario: scenario,
                phase: phase,
                number: number,
                step: step,
                tasks: tasks,
                buildScan: buildScan,
                executionTime: executionTime,
                taskExecutionTime: taskExecutionTime,
                gcTime: gcTime,
                packTime: packTime,
                unpackTime: unpackTime
            });
        }
    }

    // The event handlers to use to process builds.
    const BUILD_EVENT_HANDLERS = [PackingAndUnpackingHandler];
    
    
    // Code below is a generic utility for interacting with the Export API.
    
    class BuildProcessor {
        constructor(maxConcurrentBuildsToProcess, eventHandlerClasses, completionListener) {
            this.eventHandlerClasses = eventHandlerClasses;
            this.allHandledEventTypes = this.getAllHandledEventTypes();
            
            this.pendingBuilds = [];
            this.numBuildsInProcess = 0;
            this.maxConcurrentBuildsToProcess = maxConcurrentBuildsToProcess;
            this.completionListener = completionListener;
            this.started = false;
        }

        start(gradleEnterpriseServerUrl, startTime, withCredentials) {
            this.started = true;
            const buildStreamUrl = this.createBuildStreamUrl(gradleEnterpriseServerUrl, startTime);
            const buildStream = new EventSource(buildStreamUrl, { withCredentials: withCredentials });

            buildStream.onopen = event => console.log(`Build stream '${buildStreamUrl}' open`);
            buildStream.onerror = event => console.error('Build stream error', event);
            buildStream.addEventListener('Build', event => {
                this.enqueue(gradleEnterpriseServerUrl, JSON.parse(event.data), withCredentials);
            });
        }

        enqueue(gradleEnterpriseServerUrl, build, withCredentials) {
            build.gradleEnterpriseServerUrl = gradleEnterpriseServerUrl;
            this.pendingBuilds.push(build);
            this.processPendingBuilds(withCredentials);
        }

        processPendingBuilds(withCredentials) {
            if (this.pendingBuilds.length > 0 && this.numBuildsInProcess < this.maxConcurrentBuildsToProcess) {
                this.processBuild(this.pendingBuilds.shift(), withCredentials);
            }
        }

        createBuildStreamUrl(gradleEnterpriseServerUrl, startTime) {
            return `${gradleEnterpriseServerUrl}/build-export/v1/builds/since/${startTime}?stream`;
        }

        // Inspect the methods on the handler class to find any event handlers that start with 'on' followed by the event type like 'onBuildStarted'.
        // Then take the part of the method name after the 'on' to get the event type.
        getHandledEventTypesForHandlerClass(handlerClass) {
            return Object.getOwnPropertyNames(handlerClass.prototype)
              .filter(methodName => methodName.startsWith('on'))
              .map(methodName => methodName.substring(2));
        }

        getAllHandledEventTypes() {
            return new Set(this.eventHandlerClasses.reduce((eventTypes, handlerClass) => eventTypes.concat(this.getHandledEventTypesForHandlerClass(handlerClass)), []));
        }

        createBuildEventStreamUrl(gradleEnterpriseServerUrl, buildId) {
            const types = [...this.allHandledEventTypes].join(',');
            return `${gradleEnterpriseServerUrl}/build-export/v1/build/${buildId}/events?eventTypes=${types}`;
        }

        // Creates a map of event type -> handler instance for each event type supported by one or more handlers.
        createBuildEventHandlers(build) {
            return this.eventHandlerClasses.reduce((handlers, handlerClass) => {
                const addHandler = (type, handler) => handlers[type] ? handlers[type].push(handler) : handlers[type] = [handler];

                const handler = new handlerClass.prototype.constructor(build);

                this.getHandledEventTypesForHandlerClass(handlerClass).forEach(eventType => addHandler(eventType, handler));

                if (Object.getOwnPropertyNames(handlerClass.prototype).includes('complete')) {
                    addHandler('complete', handler);
                }

                return handlers;
            }, {});
        }

        processBuild(build, withCredentials) {
            this.numBuildsInProcess++;
            const buildEventHandlers = this.createBuildEventHandlers(build);
            const buildEventStream = new EventSource(this.createBuildEventStreamUrl(build.gradleEnterpriseServerUrl, build.buildId), { withCredentials: withCredentials });
            let buildEventStreamOpen = true;

            buildEventStream.addEventListener('BuildEvent', event => {
                const buildEventPayload = JSON.parse(event.data);
                const { eventType } = buildEventPayload.type;

                if (this.allHandledEventTypes.has(eventType)) {
                    buildEventHandlers[eventType].forEach(handler => handler[`on${eventType}`](buildEventPayload));
                }
            });

            // there isn't an onclose event that we can listen to, but an error event does get triggered
            // when the end of the stream is reached so use that as a rough proxy for the end of the stream
            buildEventStream.onerror = event => {
                if (buildEventStreamOpen) {
                    this.finishedProcessingBuild();

                    // Call the 'complete()' method on any handler that has it.
                    if (buildEventHandlers.complete) {
                        buildEventHandlers.complete.forEach(handler => handler.complete(this.completionListener));
                    }

                    buildEventStream.close();
                    buildEventStreamOpen = false;
                }
            }
        }

        finishedProcessingBuild() {
            this.numBuildsInProcess--;
            setTimeout(() => this.processPendingBuilds(), 0); // process the next set of pending builds, if any
        }
    }; 

	module.factory("gradleEnterpriseServer", function () {
		return function (completionListener) {
		    const buildProcessor = new BuildProcessor(
		      MAX_CONCURRENT_BUILDS_TO_PROCESS, 
		      BUILD_EVENT_HANDLERS,
		      completionListener
		    );
		    return buildProcessor;
		};
	});
})();
