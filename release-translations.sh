#!/bin/bash
BRANCH=master
USER="$L10N_USER"
PASS="$L10N_PASSWORD"

XWIKI_TRUNKS=`pwd`

function fix_author() {
    find ./ -name '*.xml' -exec sed -i -e 's#<creator>XWiki.Admin</creator>#<creator>xwiki:XWiki.Admin</creator>#' -e 's#<author>XWiki.Admin</author>#<author>xwiki:XWiki.Admin</author>#' -e 's#<contentAuthor>XWiki.Admin</contentAuthor>#<contentAuthor>xwiki:XWiki.Admin</contentAuthor>#' {} \; -print
}

function do_one() {
    wget $1 --user="${USER}" --password="${PASS}" --auth-no-challenge -O ./translations.zip &&
    unzip -o translations.zip &&
    rm translations.zip || $(git clean -dxf && exit -1)
    fix_author
}

function read_user_and_password() {
    if [[ -z "$USER" || -z "$PASS" ]]; then
        printf "\033[0;32mEnter your l10n.xwiki.org credentials:\033[0m\n"
        read -e -p "user> " USER
        read -e -s -p "pass> " PASS
        printf "\n"
    fi

    if [[ -z "$USER" || -z "$PASS" ]]; then
      printf "\033[1;31mPlease provide both user and password in order to be able to get the translations from l10n.xwiki.org.\033[0m\n"
      exit -1
    fi
}

function format_xar() {
    ## due to https://github.com/mycila/license-maven-plugin/issues/37 we need to perform "mvn xar:format" twice.
    mvn xar:format
    mvn xar:format
}

function do_all() {
    read_user_and_password

    if [[-f ${XWIKI_TUNKS}/application-blog-notification ]]; then
	cd ${XWIKI_TRUNKS}/application-blog-notification/src/main/resources/ || exit -1
	do_one 'http://l10n.xwiki.org/xwiki/bin/view/L10NCode/GetTranslationFile?name=Contrib.Blog%20API%20Resources&app=Contrib'
    fi

    cd ${XWIKI_TRUNKS}/application-blog-ui/src/main/resources/ || exit -1
    do_one 'http://l10n.xwiki.org/xwiki/bin/view/L10NCode/GetTranslationFile?name=Contrib.BlogTranslations&app=Contrib'
    cd ${XWIKI_TRUNKS}/application-blog-ui/ && format_xar

    git status
    printf "\033[0;32mIf there are untracked files, something probably went wrong.\033[0m\n"
}

function check_clean() {
	if [[ `git status --porcelain` ]]; then
    	# changes
    	git status
    	printf "\033[1;31mPlease do something with these changes first.\033[0m\n"
    	printf "\033[1;31min `pwd`\033[0m\n"
     	exit -1;
  	else
	    git reset --hard &&
    	git checkout ${BRANCH} &&
    	git reset --hard &&
    	git clean -dxf &&
    	git pull origin ${BRANCH} || exit -1
	fi
}

function commit() {
    MSG="[release] Updated translations."
    git add . && git commit  -m "${MSG}" && git push
}

if [[ $1 == 'commit' ]]; then
    commit
elif [[ $1 == 'clean' ]]; then
    git reset --hard && git clean -dxf
else
    check_clean
    do_all
fi

