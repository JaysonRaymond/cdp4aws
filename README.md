cdp4aws: Cloud Design Patterns for AWS
================

This project is a collection of implementations of Cloud Design Patterns using [Amazon Web Services (AWS)](http://aws.amazon.com/). 
In the spirit of agile development, these are being iteratively developed, with the first pattern implemented here being 
the [Request Queueing Cloud Design Pattern] (https://docs.google.com/document/d/1J7YDJZtkwEjNyEvweDyh2HbEIKAotk59vdOTgClqf-w/edit?usp=sharing#heading=h.fndz2kidwmz6) 
leveraging AWS [SQS](http://aws.amazon.com/sqs/) &amp; [Elastic Beanstalk](http://aws.amazon.com/elasticbeanstalk/).

The artifacts produced by this project thus far are a JavaScript library for clients, a Java library for server 
implementations, and a Maven Archetype to rapidly add a pattern to your project. 

See:
* [Demo](http://htmlpreview.github.io/?https://raw.githubusercontent.com/JaysonRaymond/cdp4aws/master/examples/request-queueing/src/main/html/index.html)
* [Design](https://docs.google.com/document/d/1oMxaCYw9y92NyhHaNvngzDPVJgP3CiyiB3jFYic4ZjI/edit?usp=sharing)

Example Sources:
* [Client-side](https://github.com/JaysonRaymond/cdp4aws/blob/master/examples/request-queueing/src/main/html/index.html#L243)
* [Worker-node](https://github.com/JaysonRaymond/cdp4aws/blob/master/examples/request-queueing/src/main/java/CommandProcessor.java#L78)


