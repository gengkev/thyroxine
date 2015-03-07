package com.desklampstudios.thyroxine.directory;

import android.support.annotation.NonNull;

public class DirectoryInfo {
    public final int iodineUid;
    @NonNull public final String tjhsstId;
    public final int graduationYear;

    @NonNull public final Name name;

    private DirectoryInfo(Builder builder) {
        this.iodineUid = builder.iodineUid;
        this.tjhsstId = builder.tjhsstId;
        this.graduationYear = builder.graduationYear;

        this.name = new Name(builder.givenName, builder.middleName,
                builder.surname, builder.nickname);
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("DirectoryInfo[iodineUid=%d, tjhsstId=%s, name=%s, graduationYear=%d]",
                iodineUid, tjhsstId, name, graduationYear);
    }

    public static class Builder {
        private int iodineUid = -1;
        @NonNull private String tjhsstId = "";
        private int graduationYear;

        @NonNull private String givenName = "";
        @NonNull private String middleName = "";
        @NonNull private String surname = "";
        @NonNull private String nickname = "";

        public Builder() { }

        public Builder iodineUid(int iodineUid) {
            this.iodineUid = iodineUid;
            return this;
        }
        public Builder tjhsstId(@NonNull String tjhsstId) {
            this.tjhsstId = tjhsstId;
            return this;
        }
        public Builder graduationYear(int graduationYear) {
            this.graduationYear = graduationYear;
            return this;
        }

        public Builder givenName(@NonNull String givenName) {
            this.givenName = givenName;
            return this;
        }
        public Builder middleName(@NonNull String middleName) {
            this.middleName = middleName;
            return this;
        }
        public Builder surname(@NonNull String surname) {
            this.surname = surname;
            return this;
        }
        public Builder nickname(@NonNull String nickname) {
            this.givenName = nickname;
            return this;
        }

        public DirectoryInfo build() {
            DirectoryInfo info = new DirectoryInfo(this);
            /*
            if (info.graduationYear < 1900 || info.graduationYear > 2200) {
                throw new IllegalArgumentException("graduation year " + info.graduationYear + " invalid");
            }
            if (info.name.givenName.isEmpty() || info.name.surname.isEmpty()) {
                throw new IllegalArgumentException("name " + info.name + " is missing given name/surname");
            }
            */
            return info;
        }
    }

    public static class Name {
        @NonNull public final String givenName;
        @NonNull public final String middleName;
        @NonNull public final String surname;
        @NonNull public final String nickname;

        private Name(@NonNull String givenName, @NonNull String middleName,
                     @NonNull String surname, @NonNull String nickname) {
            this.givenName = givenName;
            this.middleName = middleName;
            this.surname = surname;
            this.nickname = nickname;
        }

        public String getCommonName() {
            String name = " " + surname;
            if (!nickname.isEmpty()) {
                name = nickname + name;
            } else {
                name = givenName + name;
            }
            return name;
        }
        public String getFullName() {
            String name = givenName + " ";
            if (!middleName.isEmpty()) {
                name += middleName + " ";
            }
            if (!nickname.isEmpty()) {
                name += "(" + nickname + ") ";
            }
            name += surname;
            return name;
        }

        @NonNull
        @Override
        public String toString() {
            return getFullName();
        }
    }

}
