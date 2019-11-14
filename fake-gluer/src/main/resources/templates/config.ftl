<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <link href="/css/main.css" rel="stylesheet">
</head>
<body>
<h1>Fake Gluer configuration</h1>
<#if applications?has_content>
    <ul>
    <#list applications?keys as key>
        <li>application: <b>${key}</b>
            <ul>
            <#list applications[key] as instancesByName>
                <li>${instancesByName}</li>
            </#list>
            </ul>
        </li>
    </#list>
    </ul>
</#if>
<script src="/js/main.js"></script>
</body>
</html>
