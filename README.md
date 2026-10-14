# Blog Application

Create and manage blog posts.

* Project Lead: [Alex Cotiugă](http://www.xwiki.org/xwiki/bin/view/XWiki/acotiuga)
* [Documentation & Download](http://extensions.xwiki.org/xwiki/bin/view/Extension/Blog+Application)
* [Issue Tracker](https://jira.xwiki.org/browse/BLOG)
* Communication: [Mailing List](http://dev.xwiki.org/xwiki/bin/view/Community/MailingLists), [IRC](http://dev.xwiki.org/xwiki/bin/view/Community/IRC)
* [Development Practices](http://dev.xwiki.org)
* Minimal XWiki version supported: XWiki 14.10.20
* License: LGPL 2.1
* Translations: [![Translation status](https://l10n.xwiki.org/widget/xwiki-contrib/blog-translations/svg-badge.svg)](https://l10n.xwiki.org/projects/xwiki-contrib/blog-translations/)
* Sonar Dashboard: N/A
* Continuous Integration Status: [![Build Status](https://ci.xwiki.org/job/XWiki%20Contrib/job/application-blog/job/master/badge/icon)](https://ci.xwiki.org/job/XWiki%20Contrib/job/application-blog/job/master/)

# Release

* Release

```
mvn release:prepare -Pintegration-tests,legacy,docker
mvn release:perform -Pintegration-tests,legacy,docker
```

* Update https://extensions.xwiki.org/xwiki/bin/view/Extension/Blog+Application
