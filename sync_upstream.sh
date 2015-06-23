#!/bin/bash
git remote add upstream https://github.com/cantara/Whydah-UserIdentityBackend.git
git fetch upstream
git merge upstream/master
git push
