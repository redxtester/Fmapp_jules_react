#!/bin/bash

# Exit on error
set -e

# Check if running on Ubuntu
if [ ! -f /etc/os-release ]; then
    echo "Cannot determine OS."
    exit 1
fi

source /etc/os-release

if [ "$ID" != "ubuntu" ]; then
    echo "This script is for Ubuntu only."
    exit 1
fi

echo "Detected Ubuntu ${VERSION_ID}."

# Construct download URL and filename
PACKAGE_URL_VERSION="xUbuntu_${VERSION_ID}"
DEB_FILENAME="megacmd-${PACKAGE_URL_VERSION}_amd64.deb"
DOWNLOAD_URL="https://mega.nz/linux/repo/${PACKAGE_URL_VERSION}/amd64/${DEB_FILENAME}"

# Download
echo "Downloading from ${DOWNLOAD_URL}"
# Remove .deb file if it exists to ensure it is overwritten.
rm -f "${DEB_FILENAME}"
wget -q --show-progress -O "${DEB_FILENAME}" "${DOWNLOAD_URL}"

# Install
echo "Installing ${DEB_FILENAME}"
sudo apt-get update
sudo apt-get install -y "./${DEB_FILENAME}"

# Cleanup
echo "Removing ${DEB_FILENAME}"
rm "${DEB_FILENAME}"

echo "MEGAcmd has been installed."

echo "Attempting to log in..."
for i in {1..3}; do
    echo -n "Please enter the username: "
    read username
    echo -n "Please enter the password: "
    read -s password
    echo
    if mega-login $username "$password"; then
        echo "Login successful."
        break
    else
        if [ $i -lt 3 ]; then
            echo "Login failed. Please try again. You have $((3-i)) chances left."
        else
            echo "Login failed after 3 attempts. Exiting."
            exit 1
        fi
    fi
done

echo "Downloading the /bot folder..."
mega-get /bot bot

cd bot

echo "Starting synchronization from local 'bot' folder to remote '/gcloud_bot' folder..."
mega-sync . /gcloud_bot &
