#include "Errors.h"
#include "stream_proto_utils.h"
#include "string_utils.h"

#include <iomanip>
#include <iostream>
#include <sstream>

using namespace android::stream_proto;
using namespace google::protobuf::io;
using namespace std;

const bool GENERATE_MAPPING = true;

static string
make_filename(const FileDescriptorProto& file_descriptor)
{
    return file_descriptor.name() + ".h";
}

static void
write_enum(stringstream& text, const EnumDescriptorProto& enu, const string& indent)
{
    const int N = enu.value_size();
    text << indent << "// enum " << enu.name() << endl;
    for (int i=0; i<N; i++) {
        const EnumValueDescriptorProto& value = enu.value(i);
        text << indent << "const int "
                << make_constant_name(value.name())
                << " = " << value.number() << ";" << endl;
    }

    if (GENERATE_MAPPING) {
        string name = make_constant_name(enu.name());
        string prefix = name + "_";
        text << indent << "static const int _ENUM_" << name << "_COUNT = " << N << ";" << endl;
        text << indent << "static const char* _ENUM_" << name << "_NAMES[" << N << "] = {" << endl;
        for (int i=0; i<N; i++) {
            text << indent << INDENT << "\"" << stripPrefix(enu.value(i).name(), prefix) << "\"," << endl;
        }
        text << indent << "};" << endl;
        text << indent << "static const int _ENUM_" << name << "_VALUES[" << N << "] = {" << endl;
        for (int i=0; i<N; i++) {
            text << indent << INDENT << make_constant_name(enu.value(i).name()) << "," << endl;
        }
        text << indent << "};" << endl;
    }

    text << endl;
}

static void
write_field(stringstream& text, const FieldDescriptorProto& field, const string& indent)
{
    string optional_comment = field.label() == FieldDescriptorProto::LABEL_OPTIONAL
            ? "optional " : "";
    string repeated_comment = field.label() == FieldDescriptorProto::LABEL_REPEATED
            ? "repeated " : "";
    string proto_type = get_proto_type(field);
    string packed_comment = field.options().packed()
            ? " [packed=true]" : "";
    text << indent << "// " << optional_comment << repeated_comment << proto_type << ' '
            << field.name() << " = " << field.number() << packed_comment << ';' << endl;

    text << indent << "const uint64_t " << make_constant_name(field.name()) << " = 0x";

    ios::fmtflags fmt(text.flags());
    text << setfill('0') << setw(16) << hex << get_field_id(field);
    text.flags(fmt);

    text << "LL;" << endl;

    text << endl;
}

static void
write_message(stringstream& text, const DescriptorProto& message, const string& indent)
{
    int N;
    const string indented = indent + INDENT;

    text << indent << "// message " << message.name() << endl;
    text << indent << "namespace " << message.name() << " {" << endl;

    // Enums
    N = message.enum_type_size();
    for (int i=0; i<N; i++) {
        write_enum(text, message.enum_type(i), indented);
    }

    // Nested classes
    N = message.nested_type_size();
    for (int i=0; i<N; i++) {
        write_message(text, message.nested_type(i), indented);
    }

    // Fields
    N = message.field_size();
    for (int i=0; i<N; i++) {
        write_field(text, message.field(i), indented);
    }

    if (GENERATE_MAPPING) {
        N = message.field_size();
        text << indented << "static const int _FIELD_COUNT = " << N << ";" << endl;
        text << indented << "static const char* _FIELD_NAMES[" << N << "] = {" << endl;
        for (int i=0; i<N; i++) {
            text << indented << INDENT << "\"" << message.field(i).name() << "\"," << endl;
        }
        text << indented << "};" << endl;
        text << indented << "static const uint64_t _FIELD_IDS[" << N << "] = {" << endl;
        for (int i=0; i<N; i++) {
            text << indented << INDENT << make_constant_name(message.field(i).name()) << "," << endl;
        }
        text << indented << "};" << endl << endl;
    }

    text << indent << "} //" << message.name() << endl;
    text << endl;
}

static void write_header_file(const string& request_parameter, CodeGeneratorResponse* response,
                              const FileDescriptorProto& file_descriptor) {
    stringstream text;

    text << "// Generated by protoc-gen-cppstream. DO NOT MODIFY." << endl;
    text << "// source: " << file_descriptor.name() << endl << endl;

    string header = "ANDROID_" + replace_string(file_descriptor.name(), '/', '_');
    header = replace_string(header, '.', '_') + "_stream_h";
    header = make_constant_name(header);

    text << "#ifndef " << header << endl;
    text << "#define " << header << endl;
    text << endl;

    vector<string> namespaces = split(file_descriptor.package(), '.');
    for (vector<string>::iterator it = namespaces.begin(); it != namespaces.end(); it++) {
        text << "namespace " << *it << " {" << endl;
    }
    text << endl;

    size_t N;
    N = file_descriptor.enum_type_size();
    for (size_t i=0; i<N; i++) {
        write_enum(text, file_descriptor.enum_type(i), "");
    }

    N = file_descriptor.message_type_size();
    for (size_t i=0; i<N; i++) {
        write_message(text, file_descriptor.message_type(i), "");
    }

    for (vector<string>::reverse_iterator it = namespaces.rbegin(); it != namespaces.rend(); it++) {
        text << "} // " << *it << endl;
    }

    text << endl;
    text << "#endif // " << header << endl;

    if (request_parameter.find("experimental_allow_proto3_optional") != string::npos) {
        response->set_supported_features(CodeGeneratorResponse::FEATURE_PROTO3_OPTIONAL);
    }
    CodeGeneratorResponse::File* file_response = response->add_file();
    file_response->set_name(make_filename(file_descriptor));
    file_response->set_content(text.str());
}

int main(int argc, char const *argv[])
{
    (void)argc;
    (void)argv;

    GOOGLE_PROTOBUF_VERIFY_VERSION;

    CodeGeneratorRequest request;
    CodeGeneratorResponse response;

    // Read the request
    request.ParseFromIstream(&cin);

    // Build the files we need.
    const int N = request.proto_file_size();
    for (int i=0; i<N; i++) {
        const FileDescriptorProto& file_descriptor = request.proto_file(i);
        if (should_generate_for_file(request, file_descriptor.name())) {
            write_header_file(request.parameter(), &response, file_descriptor);
        }
    }

    // If we had errors, don't write the response. Print the errors and exit.
    if (ERRORS.HasErrors()) {
        ERRORS.Print();
        return 1;
    }

    // If we didn't have errors, write the response and exit happily.
    response.SerializeToOstream(&cout);

    /* code */
    return 0;
}
