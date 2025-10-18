# Blog Application

Create and manage blog posts.

* Project Lead: [Alex Cotiugă](http://www.xwiki.org/xwiki/bin/view/XWiki/acotiuga)
* [Documentation & Download](http://extensions.xwiki.org/xwiki/bin/view/Extension/Blog+Application)
* [Issue Tracker](https://jira.xwiki.org/browse/BLOG)
* Communication: [Mailing List](http://dev.xwiki.org/xwiki/bin/view/Community/MailingLists), [IRC](http://dev.xwiki.org/xwiki/bin/view/Community/IRC)
* [Development Practices](http://dev.xwiki.org)
* Minimal XWiki version supported: XWiki 12.10.3
* License: LGPL 2.1
* Translations: [![Translation status](https://l10n.xwiki.org/widgets/xwiki-contrib/-/blog-translations/svg-badge.svg)](https://l10n.xwiki.org/projects/xwiki-contrib/blog-translations/)
* Sonar Dashboard: N/A
* Continuous Integration Status: [![Build Status](http://ci.xwiki.org/job/XWiki%20Contrib/job/application-blog/job/master/badge/icon)](http://ci.xwiki.org/job/XWiki%20Contrib/job/application-blog/job/master/)

# Release

* Release

```
mvn release:prepare -Pintegration-tests,legacy
mvn release:perform -Pintegration-tests,legacy
```

* Update https://extensions.xwiki.org/xwiki/bin/view/Extension/Blog+Application
